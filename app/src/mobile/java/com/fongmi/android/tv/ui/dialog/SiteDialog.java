package com.fongmi.android.tv.ui.dialog;

import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.ImportedSites;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.List;

public class SiteDialog extends BaseAlertDialog implements SiteAdapter.OnClickListener {

    private DialogSiteBinding binding;
    private SiteListener listener;
    private SiteAdapter adapter;
    private boolean search;
    private boolean change;

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getFileUri(result, this::onJarPicked));

    public static SiteDialog create() {
        return new SiteDialog();
    }

    public SiteDialog search() {
        search = true;
        return this;
    }

    public SiteDialog change() {
        change = true;
        return this;
    }

    public void show(Fragment fragment) {
        show(fragment.getChildFragmentManager(), null);
        if (fragment instanceof SiteListener) listener = (SiteListener) fragment;
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSiteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        adapter = new SiteAdapter(this);
        binding.recycler.setAdapter(adapter);
        adapter.search(search).change(change);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(VodConfig.getHomeIndex()));
        binding.importJar.setOnClickListener(v -> FileChooser.from(launcher).show());
    }

    private void onJarPicked(Uri uri) {
        Task.execute(() -> {
            try {
                File local = FileChooser.getLocalFile(uri);
                if (local == null && uri != null && uri.getPath() != null) local = new File(uri.getPath());
                if (local == null) throw new IllegalArgumentException("Cannot resolve jar file");
                List<ImportedSites.Entry> added = ImportedSites.importJar(local);
                App.post(() -> {
                    if (added.isEmpty()) {
                        Notify.show(R.string.site_import_empty);
                    } else {
                        VodConfig.get().refreshImported();
                        dismiss();
                        Notify.show(getString(R.string.site_import_success, added.size()));
                    }
                });
            } catch (Throwable e) {
                App.post(() -> Notify.show(R.string.site_import_failed));
            }
        });
    }

    @Override
    public void onTextClick(Site item) {
        if (listener != null) listener.setSite(item);
        dismiss();
    }

    @Override
    public void onSearchClick(int position, Site item) {
        item.setSearchable(!item.isSearchable()).saveSettings();
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onChangeClick(int position, Site item) {
        item.setChangeable(!item.isChangeable()).saveSettings();
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onSearchLongClick(Site item) {
        boolean result = !item.isSearchable();
        List<Site> sites = adapter.getItems();
        sites.forEach(site -> site.setSearchable(result));
        Site.saveSettings(sites);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        Notify.show(getString(R.string.dialog_action_bulk_result, getString(R.string.dialog_action_site_search), getString(result ? R.string.setting_on : R.string.setting_off)));
        return true;
    }

    @Override
    public boolean onChangeLongClick(Site item) {
        boolean result = !item.isChangeable();
        List<Site> sites = adapter.getItems();
        sites.forEach(site -> site.setChangeable(result));
        Site.saveSettings(sites);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        Notify.show(getString(R.string.dialog_action_bulk_result, getString(R.string.dialog_action_site_change), getString(result ? R.string.setting_on : R.string.setting_off)));
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter.getItemCount() == 0) dismiss();
        else if (ResUtil.isLand(requireContext())) setWidth(0.5f);
    }
}
