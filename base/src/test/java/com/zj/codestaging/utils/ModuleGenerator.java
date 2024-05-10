package com.zj.codestaging.utils;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 模块生成操作的封装,可视化界面移步ui/ModuleManager
 * @see com.zj.codestaging.ui.ModuleManager#main
 * @Author zhijian
 * @Date 2024-02
 */

public class ModuleGenerator {

    public void load(String moduleName){
        Map<String, String> moduleInfo = getModuleInfo(moduleName);
        if (moduleInfo != null) {
            // 子模块信息
            String groupId = moduleInfo.get("groupId");
            String version = moduleInfo.get("version");
            generate(null, moduleName, groupId, version, null);
        }
    }

    /**
     * 生成子模块
     * @param templateName
     * @param moduleName
     * @param groupId
     * @param version
     * @param placeHolder 额外的字符映射(如果是null则仅加载已有子模块)
     */
    public void generate(String templateName, String moduleName, String groupId, String version, Map<String, String> placeHolder){
        String classPrefix = Character.toUpperCase(moduleName.charAt(0)) + moduleName.substring(1);
        Map<String, String> placeHolders = new HashMap<>();
        placeHolders.put("groupId", groupId);
        placeHolders.put("artifactId", "project-"+moduleName);
        placeHolders.put("version", version);
        placeHolders.put("classPrefix", classPrefix);
        placeHolders.put("moduleName", moduleName);

        if(placeHolder != null) {
            placeHolders.putAll(placeHolder);

            // 只有这些后缀会检测占位符其它的除了一些特定文件都会直接复制
            String[] editableSuffix = {"java", "properties", "yml", "xml", "txt"};

            // 生成子模块
            generateModule(templateName, moduleName, placeHolders, editableSuffix);
        }
        // 修改父工程的POM配置
        updatePOMFile(moduleName, placeHolders);

        // 修改base模块的yml配置,用来引入子模块的yml配置文件
        updateYMLFile(moduleName, false);
    }

    /**
     * 列出自定义的子模块(已加载的)
     * @return
     */
    public List<String> listLoadedChildren(){
        String rootPath = System.getProperty("user.dir");
        String basePom = rootPath + File.separator + "pom.xml";
        Model baseModel = getPomModel(basePom);
        String rootPom = rootPath + File.separator + baseModel.getParent().getRelativePath();
        Model rootModel = getPomModel(rootPom);
        List<String> rootModules = rootModel.getModules().stream().filter(item -> item.contains("project-")).collect(Collectors.toList());
        for (int i = 0; i < rootModules.size(); i++) {
            rootModules.set(i, rootModules.get(i).replace("project-", ""));
        }
        return rootModules;
    }

    /**
     * 列出自定义的子模块(所有的)
     * @return
     */
    public List<String> listAllChildren(){
        List<String> rootModules = new ArrayList<>();
        String rootPath = System.getProperty("user.dir");
        String projectPath = rootPath + File.separator + ".." + File.separator;
        File[] files = new File(projectPath).listFiles();
        for (int i = 0; i < files.length; i++) {
            if(files[i].isDirectory() && files[i].getName().contains("project-")){
                rootModules.add(files[i].getName().replace("project-", ""));
            }
        }
        return rootModules;
    }

    /**
     * 生成子模块文件
     * @param templateName 模板名
     * @param moduleName 模块名(纯英文变量)
     * @param placeHolders 占位符替换内容
     * @param editableSuffix 只替换指定后缀的文件
     */
    public void generateModule(String templateName, String moduleName, Map<String, String> placeHolders, String[] editableSuffix){
        moduleName = "project-" + moduleName;

        // 除了检查这些文件里是否有占位符，其它文件直接复制

        // 模板文件夹
        String rootPath = System.getProperty("user.dir");
        if(!rootPath.endsWith("base"))
            rootPath = rootPath + "/base";

        rootPath = rootPath + File.separator + ".." + File.separator;

        // 目标文件夹
        File targetDir =new File(rootPath + moduleName);
        if(!targetDir.exists())
            targetDir.mkdir();

        // 复制整块模板
        try {
            copyFilesRecursively(new File(rootPath + "template-" + templateName), targetDir);
        } catch (IOException e) {
            System.err.println("[err] 生成模块时复制文件出错!");
            e.printStackTrace();
            return;
        }

        // 文件列表
        List<File> allFiles = new ArrayList<>();
        getAllFile(new File(rootPath + moduleName), allFiles);

        // 先修改文件
        int size = allFiles.size();
        for (int i = 0; i < size; ++ i){
            File file = allFiles.get(i);
            if(file.isDirectory()) continue;
            String filename = Paths.get(file.getName()).getFileName().toString();
            for (int j=0;j<editableSuffix.length;++j){
                if(filename.contains(editableSuffix[j])){
                    // 替换文件信息
                    fileContainPlaceholder(moduleName, file, placeHolders);
                    break;
                }
            }
        }

        // 修改文件夹
        for (int i = 0; i < size; ++ i){
            File file = allFiles.get(i);
            if(!file.isDirectory()) continue;

            // 替换文件夹名称
            fileContainPlaceholder(moduleName, file, placeHolders);
        }
    }

    /**
     * 获取文件列表
     * @param fileInput
     * @param allFileList
     */
     void getAllFile(File fileInput, List<File> allFileList) {
        // 获取文件列表
        File[] fileList = fileInput.listFiles();
        assert fileList != null;
        for (File file : fileList) {
            allFileList.add(file);
            if (file.isDirectory()) {
                // 递归处理文件夹
                // 如果不想统计子文件夹则可以将下一行注释掉
                getAllFile(file, allFileList);
            }
        }
    }

    /**
     * 更新父工程的pom文件
     * @param moduleName
     * @param placeHolders
     */
    void updatePOMFile(String moduleName, Map<String, String> placeHolders){
        moduleName = "project-" + moduleName;

        String rootPath = System.getProperty("user.dir");
        if(!rootPath.endsWith("base"))
            rootPath = rootPath + "/base";

        String basePom = rootPath + File.separator + "pom.xml";
        Model baseModel = getPomModel(basePom);
        List<Dependency> baseDependencies = baseModel.getDependencies();
        if(!baseDependencies.stream().filter(e -> placeHolders.get("artifactId").equals(e.getArtifactId())).findFirst().isPresent()){
            Dependency dependency = new Dependency();
            dependency.setGroupId(placeHolders.get("groupId"));
            dependency.setArtifactId(placeHolders.get("artifactId"));
            dependency.setVersion(placeHolders.get("version"));
            baseDependencies.add(dependency);
            baseModel.setDependencies(baseDependencies);

            // 写入base pom文件
            MavenXpp3Writer writer = new MavenXpp3Writer();
            try {
                writer.write(new FileWriter(basePom), baseModel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String rootPom = rootPath + File.separator + baseModel.getParent().getRelativePath();
        Model rootModel = getPomModel(rootPom);
        List<String> rootModules = rootModel.getModules();
        if(!rootModules.contains(moduleName)){
            rootModules.add(moduleName);
            rootModel.setModules(rootModules);

            // 写入root pom文件
            MavenXpp3Writer writer = new MavenXpp3Writer();
            try {
                writer.write(new FileWriter(rootPom), rootModel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 卸载子模块
     * @param moduleName
     */
    public void unloadModule(String moduleName){
        StringBuilder name = new StringBuilder("project-" + moduleName);

        String rootPath = System.getProperty("user.dir");
        if(!rootPath.endsWith("base"))
            rootPath = rootPath + "/base";

        String basePom = rootPath + File.separator + "pom.xml";
        Model baseModel = getPomModel(basePom);
        List<Dependency> baseDependencies = baseModel.getDependencies().stream().filter(item -> !name.toString().equals(item.getArtifactId())).collect(Collectors.toList());
        baseModel.setDependencies(baseDependencies);

        // 写入base pom文件
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try {
            writer.write(new FileWriter(basePom), baseModel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String rootPom = rootPath + File.separator + baseModel.getParent().getRelativePath();
        Model rootModel = getPomModel(rootPom);
        List<String> rootModules = rootModel.getModules();
        if(rootModules.contains(name.toString())){
            List<String> collect = rootModules.stream().filter(item -> !item.equals(name.toString())).collect(Collectors.toList());
            rootModel.setModules(collect);

            // 写入root pom文件
            writer = new MavenXpp3Writer();
            try {
                writer.write(new FileWriter(rootPom), rootModel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        updateYMLFile(moduleName, true);
    }

    /**
     * 子模块信息
     * @param moduleName
     */
    public Map<String, String> getModuleInfo(String moduleName){
        StringBuilder name = new StringBuilder("project-" + moduleName);

        String rootPath = System.getProperty("user.dir");
        if(!rootPath.endsWith("base"))
            rootPath = rootPath + "/base";

        String childPom = rootPath + File.separator + ".." + File.separator + name.toString() + File.separator +"pom.xml";
        Model childModel = getPomModel(childPom);

        if(childModel != null) {
            Map<String, String> info = new HashMap<>();
            info.put("groupId", childModel.getGroupId());
            info.put("artifactId", childModel.getArtifactId());
            info.put("version", childModel.getVersion());
            return info;
        }
        return null;
    }

    /**
     * 更新base的yml配置文件(引入子模块的配置文件)
     * @param moduleName
     * @param remove 是否移除
     */
    public void updateYMLFile(String moduleName, boolean remove){
        StringBuilder name = new StringBuilder("project-" + moduleName);

        String rootPath = System.getProperty("user.dir");
        if(!rootPath.endsWith("base"))
            rootPath = rootPath + "/base";

        String[] envs = {"local","prod","test"};
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);

        for (int i = 0; i< envs.length; ++ i){
            // 首先要判断子系统中是否有application-环境-标识.yml(前提是项目还存在)
            if(new File(rootPath + File.separator + ".." + File.separator + name).exists()){
                if(!new File(rootPath + File.separator + ".." + File.separator + name + File.separator + "src/main/resources/application-"+envs[i]+"-"+moduleName+".yml").exists()){
                    continue;
                }
            }

            File file = new File(rootPath + File.separator + "src/main/resources/application-"+envs[i]+".yml");
            // 记录yml文件里的注释
            CommentUtils.CommentHolder holder = CommentUtils.buildCommentHolder(file);

            LinkedHashMap<String,Object> loadData = null;
            try {
                String data = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                loadData = yaml.loadAs(data, LinkedHashMap.class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<String> files = (List<String>)((Map<String, Object>)((Map<String, Object>)loadData.get("spring")).get("config")).get("import");
            if(!remove)
                files.add("file:${basedir}\\..\\"+name.toString()+"\\src\\main\\resources\\application-"+envs[i]+"-"+moduleName+".yml");
            else {
                int finalI = i;
                List<String> collect = files.stream().filter(s ->
                        !s.substring(s.lastIndexOf(File.separator) + 1).equals("application-" + envs[finalI] + "-" + moduleName + ".yml")
                ).collect(Collectors.toList());
                files.clear();
                files.addAll(collect);
            }
            // 重新生成YAML字符串
            String updatedContent = yaml.dump(loadData);
            try {
                // 覆盖文件
                FileUtils.writeStringToFile(file, updatedContent, StandardCharsets.UTF_8);
                // 填充注释信息
                holder.fillComments(file);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 替换文件内容和文件名中的占位符
     * @param moduleName
     * @param placeHolders
     * @param file
     */
    void fileContainPlaceholder(String moduleName, File file, Map<String, String> placeHolders){
        StringBuilder sb = new StringBuilder();
        Pattern ptn = Pattern.compile("\\%(.*?)\\%");

        if(!file.isDirectory()) {
            // 替换内容中的占位符
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                boolean modified = false;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = ptn.matcher(line);
                    while (matcher.find()) {
                        String s = placeHolders.get(matcher.group(1).trim());
                        if (s == null)
                            s = "";

                        // replace
                        line = line.replaceAll(matcher.group(0), s);
                        modified = true;

                    }
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
                if (modified) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(sb.toString()); // 将内容写入文件
                    } catch (IOException e) {
                        System.err.println("写入文件时发生错误：" + e.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 替换文件名中的占位符
        String fileName = Paths.get(file.getName()).getFileName().toString();
        String path = file.getPath().substring(0, file.getPath().indexOf(fileName));
        Matcher matcher = ptn.matcher(fileName);
        boolean modified = false;
        while (matcher.find()){
            String s = placeHolders.get(matcher.group(1).trim());
            if(s == null)
                s = "";

            // replace
            fileName = fileName.replaceAll(matcher.group(0), s);
            modified = true;
        }
        if(modified){
            file.renameTo(new File(path + fileName));
        }

    }

    /**
     * 复制文件
     * @param sourceDir
     * @param destDir
     * @throws IOException
     */
    void copyFilesRecursively(File sourceDir, File destDir) throws IOException {
        // 跳过指定文件夹
        String[] skipDir = {"target",".git",".idea","logs"};
        Set<String> skipDirSet = Arrays.stream(skipDir).collect(Collectors.toSet());

        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                if(skipDirSet.contains(file.getName()))
                    continue;

                File subDestDir = new File(destDir, file.getName());
                if (!subDestDir.exists()) {
                    subDestDir.mkdirs();
                }
                copyFilesRecursively(file, subDestDir);
            } else {
                // 跳过指定文件
                if(Paths.get(file.getName()).getFileName().toString().contains(".iml"))
                    continue;

                Files.copy(file.toPath(), Paths.get(destDir.toString(), file.getName()));
            }
        }
    }

    /**
     * 判断字符串是否是合法变量
     * @param str
     * @return
     */
    public boolean isVariable(String str) {
        String pattern = "^[a-zA-Z_][a-zA-Z0-9_]*$";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(str);
        return matcher.matches();
    }

    /**
     * 将pom文件转成model对象
     * @param filePath
     * @return
     */
    public Model getPomModel(String filePath){
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            model = reader.read(new FileReader(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            return model;
        }
    }

}
