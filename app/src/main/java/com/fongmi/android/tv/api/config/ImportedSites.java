package com.fongmi.android.tv.api.config;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.loader.JarLoader;
import com.fongmi.android.tv.bean.Site;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportedSites {

    public static class Entry {
        public String key;
        public String name;
        public String api;
        public String jar;
    }

    private static File dir() {
        File dir = new File(Path.files(), "imported");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static File file() {
        return new File(Path.files(), "imported_sites.json");
    }

    public static List<Entry> load() {
        try {
            String json = Path.read(file());
            if (json.isEmpty()) return new ArrayList<>();
            List<Entry> entries = App.gson().fromJson(json, new TypeToken<List<Entry>>() {}.getType());
            return entries == null ? new ArrayList<>() : entries;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void save(List<Entry> entries) throws IOException {
        Path.writeAtomically(file(), App.gson().toJson(entries).getBytes());
    }

    public static String sanitize(String name) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            builder.append((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' ? c : '_');
        }
        return builder.toString();
    }

    public static List<Entry> importJar(File source) throws Exception {
        String md5 = Crypto.md5(source);
        if (md5.isEmpty()) throw new IOException("Cannot read jar file");
        List<String> apis = JarLoader.listSpiders(source);
        if (apis.isEmpty()) return new ArrayList<>();
        File target = new File(dir(), md5 + ".jar");
        if (!target.exists()) Path.writeAtomically(target, source);
        String jar = "file://" + target.getAbsolutePath();
        List<Entry> entries = load();
        List<Entry> added = new ArrayList<>();
        for (String api : apis) {
            String name = api.substring(4);
            Entry entry = new Entry();
            entry.key = "imp_" + md5.substring(0, 8) + "_" + sanitize(name);
            entry.name = name;
            entry.api = api;
            entry.jar = jar;
            if (entries.stream().noneMatch(item -> item.key != null && item.key.equals(entry.key))) {
                entries.add(entry);
                added.add(entry);
            }
        }
        if (!added.isEmpty()) save(entries);
        return added;
    }

    public static Site toSite(Entry entry) {
        Site site = new Site();
        site.setKey(entry.key);
        site.setName(entry.name);
        site.setApi(entry.api);
        site.setJar(entry.jar);
        site.setType(3);
        return site;
    }

    public static void inject(List<Site> sites) {
        for (Entry entry : load()) {
            if (entry.key == null) continue;
            if (sites.stream().noneMatch(site -> site.getKey().equals(entry.key))) sites.add(toSite(entry));
        }
    }
}
