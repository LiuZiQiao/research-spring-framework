package com.lxk.project.springframework.utils;

import sun.misc.ClassLoaderUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
/**
 * @Description TODO
 * @Author liuxiaokun@e6yun.com
 * @Created Date: 2021/5/25 9:58
 * @ClassName extractPackage
 * @Remark
 */

public class ExtractPackage {

    public static final String CLASS_SUFFIX = ".class";
//    private static final Pattern INNER_PATTERN = java.util.regex.Pattern.compile("\$(\\d+).", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     *
     * @param packageName
     * @return
     * @throws IOException
     */
    public Set<Class<?>> findCandidateComponents(String packageName) throws IOException {
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }

        Map<String, String> classMap = new HashMap<>(32);
        String path = packageName.replace(".", "/");
        Enumeration<URL> urls = findAllClassPathResources(path);
        while (urls!=null && urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                String file = URLDecoder.decode(url.getFile(), "UTF-8");
                File dir = new File(file);
                if(dir.isDirectory()){
                    parseClassFile(dir, packageName, classMap);
                }else {
                    throw new IllegalArgumentException("file must be directory");
                }
            } else if ("jar".equals(protocol)) {
                parseJarFile(url, classMap);
            }
        }

        Set<Class<?>> set = new HashSet<>(classMap.size());
        for(String key : classMap.keySet()){
            String className = classMap.get(key);
            try {
                set.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return set;
    }

    protected void parseClassFile(File dir, String packageName, Map<String, String> classMap){
        if(dir.isDirectory()){
            File[] files = dir.listFiles();
            for (File file : files) {
                parseClassFile(file, packageName, classMap);
            }
        } else if(dir.getName().endsWith(CLASS_SUFFIX)) {
            String name = dir.getPath();
            name = name.substring(name.indexOf("classes")+8).replace("\\", ".");
            System.out.println("file:"+dir+"\t class:"+name);
            addToClassMap(name, classMap);
        }
    }

    protected void parseJarFile(URL url, Map<String, String> classMap) throws IOException {
        JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if(name.endsWith(CLASS_SUFFIX)){
                addToClassMap(name.replace("/", "."), classMap);
            }
        }
    }

    private boolean addToClassMap(String name, Map<String, String> classMap){

//        if(INNER_PATTERN.matcher(name).find()){ //过滤掉匿名内部类
//            System.out.println("anonymous inner class:"+name);
//            return false;
//        }
        //内部类
        if(name.indexOf("$")>0){
            System.out.println("inner class:"+name);
        }
        //去掉.class
        if(!classMap.containsKey(name)){
            classMap.put(name, name.substring(0, name.length()-6));
            System.out.println("111:"+name.substring(0, name.length()-6));
        }
        return true;
    }

    protected Enumeration<URL> findAllClassPathResources(String path) throws IOException {
        if(path.startsWith("/")){
            path = path.substring(1);
        }
        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(path);
        return urls;
    }

    public static void main(String[] args) throws IOException {
        ExtractPackage extractPackage = new ExtractPackage();
        Set<Class<?>> candidateComponents = extractPackage.findCandidateComponents("com.lxk.project");
        candidateComponents.forEach(clazz->{
            try {
                String name = clazz.getName();
                Object o = clazz.newInstance();
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    method.invoke("a");
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }
}
