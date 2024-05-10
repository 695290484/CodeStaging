package com.zj.codestaging.ui;

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
import org.apache.maven.model.Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
        ListView<String> lvLeft = new ListView<>(mns);
        lvLeft.setEditable(false);

        VBox left = (VBox)load.lookup("#leftBox");
        if(left != null){
            left.getChildren().add(lvLeft);
        }

        ObservableList<String> nms2 = FXCollections.observableArrayList(
                allLoadedModules);
        ListView<String> lvRight = new ListView<>(nms2);
        lvRight.setEditable(false);
        VBox right = (VBox)load.lookup("#rightBox");
        if(right != null){
            right.getChildren().add(lvRight);
        }

        Button loadBtn = (Button) load.lookup("#loadBtn");
        Button unloadBtn = (Button) load.lookup("#unloadBtn");

        // 加载
        loadBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            MultipleSelectionModel<String> selectionModel = lvLeft.getSelectionModel();
            String selectedItem = selectionModel.getSelectedItem();
            if(selectedItem != null) {
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

        // Button refreshBtn = (Button) load.lookup("#refreshBtn");
        // 刷新列表
        AtomicLong lastRefreshList = new AtomicLong(0L);
        /*refreshBtn.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            lastRefreshList.set(System.currentTimeMillis());
            refreshModuleList(mg, allModules, allLoadedModules, left, right);
            SendMsg(Alert.AlertType.INFORMATION, "刷新列表完成!");
        });*/

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
            mg.generate(choosedModule[0], moduleName, groupId, version, extraParam);
            lvLeft.getItems().remove(moduleName);
            lvRight.getItems().add(moduleName);
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

        tabPane.getTabs().addAll(tab1, tab2);

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
        allLoadedModules.clear();
        allLoadedModules.addAll(listLoadedChildren(mg));
        unloadProjectNotExists(mg, allLoadedModules);
        allModules.clear();
        allModules.addAll(listAllUnloadChildren(allLoadedModules));

        ListView<String> updatedLvLeft = new ListView<>(FXCollections.observableArrayList(
                allModules));
        left.getChildren().set(1, updatedLvLeft);

        ListView<String> updatedLvRight = new ListView<>(FXCollections.observableArrayList(
                allLoadedModules));
        right.getChildren().set(1, updatedLvRight);
    }

    void SendMsg(Alert.AlertType alertType, String msg) {
        Alert alert = new Alert(alertType);
        alert.setContentText(msg);
        alert.show();
    }
}
