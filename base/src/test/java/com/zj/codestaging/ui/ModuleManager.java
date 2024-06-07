package com.zj.codestaging.ui;

import com.zj.codestaging.utils.EntityGenerator;
import com.zj.codestaging.utils.ModuleGenerator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Description 模块生成工具UI
 * @Author zhijian
 * @Date 2024-02
 */
public class ModuleManager extends Application {

    public static void main(String[] args) {
        launch();
    }

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2);

    @Override
    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
    }

    ListView<String> lvLeft;
    ListView<String> lvRight;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("系统模板管理器(开发者工具)");

        /*
        //////////////////////////////////////////////////
        *   系统管理
        /////////////////////////////////////////////////
        */
        FlowPane pane = new FlowPane();
        ObservableList<Node> children = pane.getChildren();

        FXMLLoader fxmlLoader = new FXMLLoader(ModuleManager.class.getResource("/codestaging/ui/moduleManager.fxml"));
        Parent load = (Parent)fxmlLoader.load();
        children.add(load);

        ModuleGenerator mg = new ModuleGenerator();
        List<String> allLoadedModules = listLoadedChildren(mg);
        unloadProjectNotExists(mg, allLoadedModules);
        List<String> allModules = listAllUnloadChildren(allLoadedModules);

        ObservableList<String> mns = FXCollections.observableArrayList(
                allModules);
        lvLeft = new ListView<>(mns);
        lvLeft.setEditable(false);

        VBox left = (VBox)load.lookup("#leftBox");
        if(left != null){
            left.getChildren().add(lvLeft);
        }

        ObservableList<String> nms2 = FXCollections.observableArrayList(
                allLoadedModules);
        lvRight = new ListView<>(nms2);
        lvRight.setEditable(false);
        VBox right = (VBox)load.lookup("#rightBox");
        if(right != null){
            right.getChildren().add(lvRight);
        }

        Button loadBtn = (Button) load.lookup("#loadBtn");
        Button unloadBtn = (Button) load.lookup("#unloadBtn");

        // Button refreshBtn = (Button) load.lookup("#refreshBtn");
        // 刷新列表
        AtomicLong lastRefreshList = new AtomicLong(0L);
        /*refreshBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            lastRefreshList.set(System.currentTimeMillis());
            refreshModuleList(mg, allModules, allLoadedModules, left, right);
            SendMsg(Alert.AlertType.INFORMATION, "刷新列表完成!");
        });*/

        // 加载
        loadBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            MultipleSelectionModel<String> selectionModel = lvLeft.getSelectionModel();
            String selectedItem = selectionModel.getSelectedItem();
            if(selectedItem != null) {
                lastRefreshList.set(System.currentTimeMillis());
                loadBtn.setDisable(true);
                mg.load(selectedItem);
                lvLeft.getItems().remove(selectedItem);
                lvRight.getItems().add(selectedItem);
                SendMsg(Alert.AlertType.INFORMATION, "加载子模块成功,请在IDE中刷新工程!");
                loadBtn.setDisable(false);
            }
        });

        // 卸载
        unloadBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            lastRefreshList.set(System.currentTimeMillis());
            MultipleSelectionModel<String> selectionModel = lvRight.getSelectionModel();
            String selectedItem = selectionModel.getSelectedItem();
            if(selectedItem != null) {
                unloadBtn.setDisable(true);
                mg.unloadModule(selectedItem);
                lvLeft.getItems().add(selectedItem);
                lvRight.getItems().remove(selectedItem);
                SendMsg(Alert.AlertType.INFORMATION, "卸载子模块成功,请在IDE中刷新工程!");
                unloadBtn.setDisable(false);
            }
        });

        /*
        //////////////////////////////////////////////////
        *   系统生成
        /////////////////////////////////////////////////
        */
        FlowPane pane2 = new FlowPane();
        ObservableList<Node> children2 = pane2.getChildren();

        FXMLLoader fxmlLoader2 = new FXMLLoader(ModuleManager.class.getResource("/codestaging/ui/moduleGenerator.fxml"));
        Parent load2 = (Parent)fxmlLoader2.load();
        children2.add(load2);

        Button addBtn = (Button) load2.lookup("#addBtn");

        TextArea placeholders = (TextArea) load2.lookup("#placeholders");
        // 列出所有的占位符(除了几个默认的)
        String[] hiddenKey = {"classPrefix","groupId","moduleName","artifactId","version"};
        List<String> hiddenKeyList = Arrays.asList(hiddenKey);

        // 输入框内容改变
        /*
        placeholders.textProperty().addListener((observableValue, oldValue, newValue) -> {
            String[] split = newValue.split("\n");
            System.err.println(JSON.toJSONString(split));
        });
        */

        FXMLLoader fxmlLoader3 = new FXMLLoader(ModuleManager.class.getResource("/codestaging/ui/entityGenerator.fxml"));
        Parent load3 = (Parent)fxmlLoader3.load();
        ComboBox selectTemplate3 = (ComboBox) load3.lookup("#selectModule");

        String[] choosedModule = {null};
        addBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if(null == choosedModule[0]){
                SendMsg(Alert.AlertType.ERROR, "请先选择系统模板!");
                return;
            }
            String moduleName = ((TextField) load2.lookup("#moduleName")).getText();
            if(moduleName.contains("project-")){
                SendMsg(Alert.AlertType.ERROR, "模块名不应包含'project-'");
                return;
            }
            String groupId = "com.zj"; // ((TextField) load2.lookup("#groupId")).getText();
            String version = ((TextField) load2.lookup("#version")).getText();

            if(moduleName.equals("") || version.equals("")){
                SendMsg(Alert.AlertType.ERROR, "唯一标识、版本号是必填项!");
                return;
            }

            if(!mg.isVariable(moduleName)){
                SendMsg(Alert.AlertType.ERROR, "唯一标识必需是合法变量!");
                return;
            }

            if(allLoadedModules.contains(moduleName) || allModules.contains(moduleName)){
                SendMsg(Alert.AlertType.ERROR, "唯一标识已被使用，请更换!");
                return;
            }

            String text = ((TextArea) load2.lookup("#placeholders")).getText();
            String[] split = text.split("\n");
            Map<String, String> extraParam = new HashMap<>();
            for (int i = 0; i < split.length; i++) {
                if("".equals(split[i]) || split[i].startsWith(";"))
                    continue;

                String[] split1 = split[i].split("=");
                if(split1.length<2){
                    SendMsg(Alert.AlertType.ERROR, "格式错误,应为 键名=键值!");
                    return;
                }
                extraParam.put(split1[0], split1[1]);
            }
            addBtn.setDisable(true);
            // 创建新的子模块
            lastRefreshList.set(System.currentTimeMillis());
            mg.generate(choosedModule[0], moduleName, groupId, version, extraParam);
            lvLeft.getItems().remove(moduleName);
            lvRight.getItems().add(moduleName);
            selectTemplate3.getItems().add(moduleName);
            SendMsg(Alert.AlertType.INFORMATION, "创建子模块成功,请回到IDEA中等待工程自动刷新!");

            addBtn.setDisable(false);
        });

        //ComboBox selectTemplate = (ComboBox) load2.lookup("#selectTemplate");
        // splitPane特殊的查找组件的方式
        ComboBox selectTemplate = (ComboBox) (((SplitPane)load2.lookup("#splitPane")).getItems().get(0));
        TextArea templateDesc = (TextArea) (((SplitPane)load2.lookup("#splitPane")).getItems().get(1));

        List<String> templates = listAllTemplates();
        selectTemplate.getItems().addAll(templates);
        selectTemplate.setOnAction(event -> {
            choosedModule[0] = selectTemplate.getValue().toString();

            // 模板目录
            String rootPath = System.getProperty("user.dir");
            String modulePath = rootPath + File.separator + "template-" + choosedModule[0];

            // 读取模板描述文件
            String descFile = modulePath + File.separator + "description.txt";
            File file = new File(descFile);
            if(file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(descFile))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line + "\n");
                    }
                    templateDesc.setText(content.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 列出模板占位符替换变量
            StringBuilder sb = new StringBuilder();
            Set<String> keys = new HashSet<>();
            try {
                listPlaceholders(mg, new File(modulePath), keys);
                for (String key: keys){
                    if(hiddenKeyList.contains(key)) continue;
                    sb.append(key + "=" + "\n");
                }
                placeholders.setText(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }


        });

        /*
        //////////////////////////////////////////////////
        *   实体类生成
        /////////////////////////////////////////////////
        */
        FlowPane pane3 = new FlowPane();
        ObservableList<Node> children3 = pane3.getChildren();
        children3.add(load3);

        Button addBtn3 = (Button) load3.lookup("#addBtn");

        Map<String, String> dbInfo = new HashMap<>();
        String[] choosedModule3 = {null};
        addBtn3.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if(null == choosedModule3[0]){
                SendMsg(Alert.AlertType.ERROR, "请先选择子模块!");
                return;
            }

            if(dbInfo.size()<1){
                SendMsg(Alert.AlertType.ERROR, "未找到数据源配置信息!");
                return;
            }

            String driver = dbInfo.get("driver");
            String url = dbInfo.get("url");
            String username = dbInfo.get("username");
            String password = dbInfo.get("password");
            String db = dbInfo.get("db");

            addBtn3.setDisable(true);
            try {
                EntityGenerator.generateEntity(driver, url, username, password, db, choosedModule3[0]);
            } catch (Exception e) {
                SendMsg(Alert.AlertType.ERROR, "生成实体类时出错!\n" + e.getMessage());
                return;
            }
            SendMsg(Alert.AlertType.INFORMATION, "创建实体类成功,请回到IDEA中等待工程自动刷新!");
            addBtn3.setDisable(false);
        });

        TextArea templateDesc3 = (TextArea) load3.lookup("#dbConfig");

        List<String> modules = listAllModules();
        selectTemplate3.getItems().addAll(modules);
        selectTemplate3.setOnAction(event -> {
            choosedModule3[0] = selectTemplate3.getValue().toString();

            // 子模块目录
            String rootPath = System.getProperty("user.dir");
            String modulePath = rootPath + File.separator + "project-" + choosedModule3[0];

            // 读取本地配置文件
            String configFile = modulePath + File.separator + "src/main/resources/application-local-" + choosedModule3[0] + ".yml";
            File file = new File(configFile);
            if(file.exists()) {
                DumperOptions dumperOptions = new DumperOptions();
                dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(dumperOptions);
                LinkedHashMap<String,Object> loadData = null;

                try {
                    String data = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    loadData = yaml.loadAs(data, LinkedHashMap.class);
                    Map<String, Object> map = (Map<String, Object>)((Map<String, Object>) loadData.get("spring")).get("datasource");
                    Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
                    String driver, url, username , password, db;
                    while(it.hasNext()){
                        Map.Entry<String, Object> entry = it.next();
                        Map<String, Object> value = (Map<String, Object>) entry.getValue();
                        driver = (String) value.get("driver-class-name");
                        url = (String) value.get("url");
                        username = (String) value.get("username");
                        password = (String) value.get("password");

                        int startIndex = url.lastIndexOf("/") + 1;
                        int endIndex = url.indexOf("?");
                        if (startIndex != 0 && endIndex != -1){
                            db = url.substring(startIndex, endIndex);
                            templateDesc3.setText(
                                    "driver: " + driver + "\n"
                                    + "url: " + url + "\n"
                                    + "username: " + username + "\n"
                                    + "password: " + password + "\n"
                                    + "db: " + db
                            );
                            dbInfo.put("driver", driver);
                            dbInfo.put("url", url);
                            dbInfo.put("username", username);
                            dbInfo.put("password", password);
                            dbInfo.put("db", db);

                        }else{
                            SendMsg(Alert.AlertType.ERROR, "未找到数据源的url中的数据库名称");
                        }


                        // 取完第一个数据源配置就结束
                        break;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }else{
                SendMsg(Alert.AlertType.ERROR, "未找到配置文件:" + configFile);
            }

        });

        /*
        //////////////////////////////////////////////////
        *   选项卡
        /////////////////////////////////////////////////
        */
        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("系统管理");
        tab1.setContent(pane);
        tab1.setClosable(false);
        Tab tab2 = new Tab("系统生成");
        tab2.setContent(pane2);
        tab2.setClosable(false);
        Tab tab3 = new Tab("实体类生成");
        tab3.setContent(pane3);
        tab3.setClosable(false);

        tabPane.getTabs().addAll(tab1, tab2, tab3);

        Scene scene = new Scene(tabPane, 560, 350);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);

        primaryStage.show();

        /*
        //////////////////////////////////////////////////
        *   定时线程 用来刷新模块列表
        /////////////////////////////////////////////////
        */
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() ->{
            Platform.runLater(() -> {
                if(System.currentTimeMillis() - lastRefreshList.get() < 2400L)
                   return;

                refreshModuleList(mg, allModules, allLoadedModules, left, right);
                lastRefreshList.set(System.currentTimeMillis());

                List<String> m = listAllModules();
                if(!sameList(m, modules))
                    selectTemplate3.getItems().setAll(m);
            });
        }, 2000L, 2500L, TimeUnit.MILLISECONDS);
    }

    /**
     * 根据文件夹名称列出所有未加载的子模块
     * @param allLoadedModules
     * @return
     */
    public List<String> listAllUnloadChildren(List<String> allLoadedModules){
        List<String> rootModules = new ArrayList<>();
        String rootPath = System.getProperty("user.dir");
        File[] files = new File(rootPath).listFiles();
        for (int i = 0; i < files.length; i++) {
            if(files[i].isDirectory() && files[i].getName().contains("project-")){
                String moduleName = files[i].getName().replace("project-", "");
                if(!allLoadedModules.contains(moduleName))
                    rootModules.add(moduleName);
            }
        }
        return rootModules;
    }

    public List<String> listAllModules(){
        List<String> rootModules = new ArrayList<>();
        String rootPath = System.getProperty("user.dir");
        File[] files = new File(rootPath).listFiles();
        for (int i = 0; i < files.length; i++) {
            if(files[i].isDirectory() && files[i].getName().contains("project-")){
                String moduleName = files[i].getName().replace("project-", "");
                rootModules.add(moduleName);
            }
        }
        return rootModules;
    }

    /**
     * 列出自定义的子模块(已加载的)
     * @return
     */
    public List<String> listLoadedChildren(ModuleGenerator mg){
        String rootPath = System.getProperty("user.dir");
        String basePom = rootPath + File.separator + "pom.xml";
        Model rootModel = mg.getPomModel(basePom);
        List<String> rootModules = rootModel.getModules().stream().filter(item -> item.contains("project-")).collect(Collectors.toList());
        for (int i = 0; i < rootModules.size(); i++) {
            rootModules.set(i, rootModules.get(i).replace("project-", ""));
        }
        return rootModules;
    }

    /**
     * 卸载被强行删了的子模块
     * @param mg
     * @param allLoadedModules
     */
    public void unloadProjectNotExists(ModuleGenerator mg, List<String> allLoadedModules){
        String rootPath = System.getProperty("user.dir");
        Iterator<String> iterator = allLoadedModules.iterator();
        while(iterator.hasNext()){
            String loadProject = iterator.next();
            String projectDir = rootPath + File.separator + "project-"+ loadProject;
            File file = new File(projectDir);
            if(file.exists() && file.isDirectory())
                continue;

            mg.unloadModule(loadProject);
            iterator.remove();
        }

    }

    /**
     * 列出所有系统模板
     * @return
     */
    public List<String> listAllTemplates(){
        List<String> templates = new ArrayList<>();
        String rootPath = System.getProperty("user.dir");
        File[] files = new File(rootPath).listFiles();
        for (int i = 0; i < files.length; i++) {
            if(files[i].isDirectory() && files[i].getName().contains("template-")){
                String templateName = files[i].getName().replace("template-", "");
                templates.add(templateName);
            }
        }
        return templates;
    }

    void listPlaceholders(ModuleGenerator gc, File sourceDir, Set<String> keys) throws IOException {
        // 跳过指定文件夹
        String[] skipDir = {"target",".git",".idea","logs"};
        Set<String> skipDirSet = Arrays.stream(skipDir).collect(Collectors.toSet());

        Pattern ptn = Pattern.compile("\\%(.*?)\\%");

        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                if(skipDirSet.contains(file.getName()))
                    continue;

                Matcher matcher = ptn.matcher(file.getName());
                while (matcher.find()) {
                    keys.add(matcher.group(1).trim());
                }
                listPlaceholders(gc, file, keys);
            } else {
                // 跳过指定文件
                if(Paths.get(file.getName()).getFileName().toString().contains(".iml"))
                    continue;

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = ptn.matcher(line);
                        while (matcher.find()) {
                            keys.add(matcher.group(1).trim());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 刷新模块列表
     * @param mg
     * @param allModules
     * @param allLoadedModules
     * @param left
     * @param right
     */
    void refreshModuleList(ModuleGenerator mg, List<String> allModules, List<String> allLoadedModules, VBox left, VBox right){
        List<String> listLoadedChildren = listLoadedChildren(mg);
        if(!sameList(allLoadedModules, listLoadedChildren)) {
            allLoadedModules.clear();
            allLoadedModules.addAll(listLoadedChildren);
            unloadProjectNotExists(mg, allLoadedModules);

            ListView<String> updatedLvRight = new ListView<>(FXCollections.observableArrayList(
                    allLoadedModules));
            right.getChildren().set(1, updatedLvRight);
            lvRight = updatedLvRight;
        }

        List<String> listAllUnloadChildren = listAllUnloadChildren(allLoadedModules);
        if(!sameList(allModules, listAllUnloadChildren)) {
            allModules.clear();
            allModules.addAll(listAllUnloadChildren);

            ListView<String> updatedLvLeft = new ListView<>(FXCollections.observableArrayList(
                    allModules));
            left.getChildren().set(1, updatedLvLeft);
            lvLeft = updatedLvLeft;
        }
    }

    void SendMsg(Alert.AlertType alertType, String msg) {
        Alert alert = new Alert(alertType);
        alert.setContentText(msg);
        alert.show();
    }

    boolean sameList(List<String> list1, List<String> list2){
        boolean same = true;
        if (list1.size() != list2.size()) {
            same = false;
        } else {
            for (int i = 0; i < list1.size(); i++) {
                if (!list1.get(i).equals(list2.get(i))) {
                    same = false;
                    break;
                }
            }
        }

        return same;
    }
}
