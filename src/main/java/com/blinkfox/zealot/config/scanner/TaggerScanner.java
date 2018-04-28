package com.blinkfox.zealot.config.scanner;

import com.blinkfox.zealot.config.AbstractZealotConfig;
import com.blinkfox.zealot.config.annotation.Tagger;
import com.blinkfox.zealot.config.annotation.Taggers;
import com.blinkfox.zealot.config.entity.TagHandler;
import com.blinkfox.zealot.consts.ZealotConst;
import com.blinkfox.zealot.core.IConditHandler;
import com.blinkfox.zealot.helpers.StringHelper;
import com.blinkfox.zealot.log.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * zealot Handler上的xml标签注解'@Tagger'扫描类.
 * 将扫描的类添加到'AbstractZealotConfig.tagHandlerMap'中，供后续配置使用.
 *
 * @author blinkfox on 2018/4/26.
 */
public final class TaggerScanner implements Scanner {

    private static Log log = Log.get(TaggerScanner.class);

    /** 存放所有扫描位置下的class对象的Set集合. */
    private Set<Class<?>> classSet;

    /**
     * 私有构造方法.
     */
    private TaggerScanner() {
        this.classSet = new HashSet<Class<?>>();
    }

    /**
     * 获取 TaggerScanner 最新实例的唯一方法.
     * @return TaggerScanner实例
     */
    public static TaggerScanner newInstance() {
        return new TaggerScanner();
    }

    /**
     * 扫描配置的zealot handler包下所有的class.
     * 并将含有'@Tagger'和'@Taggers'的注解的Class解析出来，存储到tagHandlerMap配置中.
     *
     * @param handlerLocations handler所在的位置
     */
    public void scan(String handlerLocations) {
        if (StringHelper.isBlank(handlerLocations)) {
            return;
        }

        // 对配置的xml路径按逗号分割的规则来解析，如果是XML文件则直接将该xml文件存放到xmlPaths的Set集合中，
        // 否则就代表是xml资源目录，并解析目录下所有的xml文件，将这些xml文件存放到xmlPaths的Set集合中，
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String[] locationArr = handlerLocations.split(ZealotConst.COMMA);
        for (String location: locationArr) {
            if (StringHelper.isBlank(location)) {
                continue;
            }

            // 判断文件如果是具体的Java文件和class文件，则将文件解析成Class对象.
            // 如果都不是，则视其为包,然后解析该包及子包下面的所有class文件.
            String cleanLocation = location.trim();
            if (StringHelper.isJavaFile(cleanLocation) || StringHelper.isClassFile(cleanLocation)) {
                this.addClassByName(classLoader, cleanLocation.substring(0, cleanLocation.lastIndexOf('.')));
            } else {
                this.addClassByPackage(classLoader, cleanLocation);
            }
        }

        this.addTagHanderInMap();
    }

    /**
     * 根据classLoader和className找到对应的class对象.
     * @param classLoader ClassLoader实例
     * @param className class全路径名
     */
    private void addClassByName(ClassLoader classLoader, String className) {
        try {
            classSet.add(classLoader.loadClass(className));
        } catch (ClassNotFoundException expected) {
            // 由于是扫描package下的class，即时出现异常，也忽略掉.
            log.warn("【警告】未找到class类:'" + className + "'，将忽略不解析此类.");
        }
    }

    /**
     * 根据包名和Classloader实例，将该包下的所有Class存放到classSet集合中.
     *
     * @param classLoader ClassLoader实例
     * @param packageName 包名
     */
    private void addClassByPackage(ClassLoader classLoader, String packageName) {
        // 根据包名和Classloader实例，得到该包的URL Enumeration.
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> urlEnum = this.getUrlsByPackge(classLoader, packageDirName);
        if (urlEnum == null) {
            return;
        }

        while (urlEnum.hasMoreElements()) {
            URL url = urlEnum.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                try {
                    this.addClassesByFile(classLoader, packageName, URLDecoder.decode(url.getFile(), "UTF-8"));
                } catch (UnsupportedEncodingException expected) {
                    // 此处不打印异常堆栈.
                    log.warn("该包结构无法转换成UTF-8的编码格式.");
                }
            } else if ("jar".equals(protocol)) {
                this.addClassByJar(classLoader, url, packageName, packageDirName);
            }
        }
    }

    /**
     * 根据包名和Classloader实例，得到该包的URL Enumeration.
     *
     * @param classLoader  ClassLoader实例
     * @param packageName 包全路径名
     * @return URL枚举
     */
    private Enumeration<URL> getUrlsByPackge(ClassLoader classLoader, String packageName) {
        try {
            return classLoader.getResources(packageName);
        } catch (IOException e) {
            // 由于是扫描package下的class，即时出现异常，也忽略掉.
            log.warn("【警告】未找到包:'" + packageName + "'下的URL，将忽略此种错误情况.");
            return null;
        }
    }

    /**
     * 以文件的形式来获取包下的所有Class.
     *
     * @param classLoader 类加载器
     * @param packageName 包名
     * @param packagePath 包路径
     */
    private void addClassesByFile(ClassLoader classLoader, String packageName, String packagePath) {
        // 获取此包的目录 建立一个File,如果不存在或者 也不是目录就直接返回
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        // 如果存在,就获取包下的所有文件,包括目录,筛选出目录和.class文件.
        File[] dirfiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return (file.isDirectory()) || StringHelper.isClassFile(file.getName());
            }
        });
        if (dirfiles == null) {
            return;
        }

        // 循环所有文件,如果是目录 则继续扫描
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                this.addClassesByFile(classLoader, packageName + "." + file.getName(), file.getAbsolutePath());
                continue;
            }

            // 如果是java类文件 去掉后面的.class,只留下类名，添加到集合中去.
            String className = file.getName().substring(0, file.getName().lastIndexOf('.'));
            this.addClassByName(classLoader, packageName + '.' + className);
        }
    }

    /**
     * 通过识别jar的形式将其下的所有class添加到classSet集合中.
     * @param classLoader 类加载器
     * @param url URL实例
     * @param packageName 报名
     * @param packageDirName 包路径名
     */
    private void addClassByJar(ClassLoader classLoader, URL url, String packageName, String packageDirName) {
        // 从url中获取jar，然后从此jar包得到一个枚举类，然后进行迭代.
        JarFile jar;
        try {
            jar = ((JarURLConnection) url.openConnection()).getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                // 如果是以/开头的,则获取后面的字符串
                if (name.charAt(0) == '/') {
                    name = name.substring(1);
                }

                // 如果前半部分和定义的包名相同.
                if (name.startsWith(packageDirName)) {
                    int index = name.lastIndexOf('/');
                    // 如果以"/"结尾，则是一个包，获取包名并把"/"替换成"."
                    if (index != -1) {
                        packageName = name.substring(0, index).replace('/', '.');
                    }
                    // 如果可以迭代下去 并且是一个包,如果是一个.class文件 而且不是目录
                    if (index != -1 && name.endsWith(".class") && !entry.isDirectory()) {
                        // 去掉后面的".class" 获取真正的类名
                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                        this.addClassByName(classLoader, packageName + '.' + className);
                    }
                }
            }
        } catch (IOException expected) {
            // 此处不打印堆栈信息.
            log.warn("从jar文件中读取class出错.");
        }
    }

    /**
     * 将扫描到的所有class进行再解析，如果其含有{@link com.blinkfox.zealot.config.annotation.Tagger}和
     * {@link com.blinkfox.zealot.config.annotation.Taggers}注解，则将其进行解析添加到tagHandlerMap中.
     */
    private void addTagHanderInMap() {
        // 循环遍历所有class，如果其不是IConditHandler.class的实现类，则表明其是正确的class，继续下次循环.
        for (Class<?> cls: classSet) {
            // 如果是 Tagger 注解，则将其 Tagger 信息存放到Map中.
            if (cls.isAnnotationPresent(Tagger.class) && isImplConditHandlerClass(cls)) {
                Class<? extends IConditHandler> conditCls = (Class<? extends IConditHandler>) cls;
                Tagger tagger = conditCls.getAnnotation(Tagger.class);
                this.addTagHandlerInMapByTagger(conditCls, tagger);
            }

            // 如果是 Taggers 注解，则解析出其下所有的Tagger来存放到Map中.
            if (cls.isAnnotationPresent(Taggers.class) && isImplConditHandlerClass(cls)) {
                Taggers taggers = cls.getAnnotation(Taggers.class);
                Tagger[] taggerArr = taggers.value();
                for (Tagger tagger: taggerArr) {
                    this.addTagHandlerInMapByTagger((Class<? extends IConditHandler>) cls, tagger);
                }
            }
        }
    }

    /**
     * 判断给定的class所对应的类是否是IConditHandler类的实现类.
     * <p>由于通过'isAssignableFrom()'判断'实现'关系时是false，所以这里采用获取'getInterfaces()'方法来判断.</p>
     *
     * @param implCls 待判断的class
     * @return 布尔值
     */
    private boolean isImplConditHandlerClass(Class<?> implCls) {
        Class<?>[] classes = implCls.getInterfaces();
        if (classes == null) {
            return false;
        }

        // 循环判断其接口是否含有'IConditHandler'接口.
        for (Class cls: classes) {
            if (IConditHandler.class.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 添加单个的Tager注解相关信息到tagHandlerMap中.
     * @param cls IConditHandler实现类的class
     */
    private void addTagHandlerInMapByTagger(Class<? extends IConditHandler> cls, Tagger tagger) {
        AbstractZealotConfig.getTagHandlerMap()
                .put(tagger.value(), new TagHandler(tagger.prefix(), cls, tagger.symbol()));
    }

}