package com.zyneonstudios.instance.creator;

import com.zyneonstudios.InstanceCreator;
import com.zyneonstudios.utils.Config;
import javafx.application.Platform;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;

public class JCefFrame extends JFrame {

    private final CefApp app;
    private Component ui;
    private final CefClient client;
    private final CefBrowser browser;
    private boolean browserFocus;
    private final CefAppBuilder builder;

    private String creator;
    private String id;
    private String name;
    private String version;
    private String minecraft;
    private String modloader;
    private String modloaderVersion;

    private String filePath;
    private URL url;

    private String json;
    private String login;

    public JCefFrame() throws UnsupportedPlatformException, CefInitializationException, IOException, InterruptedException {
        browserFocus = true;
        File installDir = new File("libs/jcef/");
        installDir.mkdirs();
        builder = new CefAppBuilder();
        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override @Deprecated
            public void stateHasChanged(CefApp.CefAppState state) {
                if (state == CefApp.CefAppState.TERMINATED) Platform.exit();
            }
        });
        builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE;
        builder.setInstallDir(installDir);
        builder.install();
        builder.getCefSettings().windowless_rendering_enabled = false;
        app = builder.build();
        client = app.createClient();
        CefMessageRouter messageRouter = CefMessageRouter.create();
        client.addMessageRouter(messageRouter);
        browser = client.createBrowser("https://danieldieeins.github.io/ZyneonApplicationContent/i/index.html", false, false);
        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                if(message.contains("[Backend-Connector] ")) {
                    String request = message.replace("[Backend-Connector] ","");
                    resolveRequest(request);
                }
                return super.onConsoleMessage(browser, level, message, source, line);
            }
        });
    }

    private void resolveRequest(String request) {
        if(request.equalsIgnoreCase("upload")) {
            SwingUtilities.invokeLater(() -> {
                filePath = InstanceCreator.getFileChooser().getZipPath();
                browser.executeJavaScript("javascript:setFilePath('" + filePath + "')", "https://danieldieeins.github.io/ZyneonApplicationContent/h/account.html", 5);
            });
        } else if(request.equalsIgnoreCase("open")) {
            openFolder();
        } else if(request.equalsIgnoreCase("publish")) {
            browser.executeJavaScript("javascript:sendJson()", "https://danieldieeins.github.io/ZyneonApplicationContent/h/account.html", 5);
        } else if(request.equalsIgnoreCase("json")) {
            SwingUtilities.invokeLater(() -> {
                json = InstanceCreator.getFileChooser().getJsonPath();
                browser.executeJavaScript("javascript:setJsonPath('" + json + "')", "https://danieldieeins.github.io/ZyneonApplicationContent/h/account.html", 5);
            });
        } else if(request.equalsIgnoreCase("export")) {
            browser.executeJavaScript("javascript:sendInput()", "https://danieldieeins.github.io/ZyneonApplicationContent/h/account.html", 5);
        } else if(request.startsWith("set.")) {
            String[] req = request.split("\\.");
            if(req.length>2) {
                String request_ = req[2];
                for(String req_:req) {
                    if(!req_.equals(req[0])&&!req_.equals(req[1])&&!req_.equals(req[2])) {
                        request_=request_+"."+req_;
                    }
                }
                resolveInput(req[1], request_);
            }
        }
    }

    public void resolveInput(String type, String value) {
        switch (type) {
            case "creator" -> creator = value;
            case "login" -> login = value;
            case "id" -> id = value;
            case "name" -> name = value;
            case "version" -> version = value;
            case "minecraft" -> minecraft = value;
            case "mlv" -> modloaderVersion = value;
            case "file" -> {
                try {
                    url = new URL(value);
                    filePath = null;
                } catch (MalformedURLException e) {
                    url = null;
                }
            }
            case "modloader" -> modloader = value;
        }
        if(url!=null||filePath!=null) {
            if(filePath!=null&&creator==null) {
                return;
            }
            if(id!=null&&name!=null&&version!=null&&minecraft!=null&&modloaderVersion!=null&&modloader!=null) {
                generate();
            }
        } else if(json!=null&&login!=null) {
            export();
        }
    }

    public void openFolder() {
        if(Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if(desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(new File(Paths.get("").toAbsolutePath().toString()));
                } catch (IOException ignore) {}
            }
        }
    }

    public boolean generate() {
        boolean success = true;

        Config export = new Config("export/"+name+"-"+version+".json");
        export.set("modpack.id",id);
        export.set("modpack.name",name);
        export.set("modpack.version",version);
        export.set("modpack.minecraft",minecraft);
        if(modloader.contains("Forge")) {
            export.set("modpack.forge.version",modloaderVersion);
            if(modloader.contains("NEU")) {
                export.set("modpack.forge.type","NEW");
            } else {
                export.set("modpack.forge.type","OLD");
            }
        } else if(modloader.contains("Fabric")) {
            export.set("modpack.fabric",modloaderVersion);
        }
        String download;
        if(url==null) {
            if(InstanceCreator.upload(creator,filePath,id+"-"+version+".zip")) {
                download = "https://raw.githubusercontent.com/danieldieeins/ZyneonApplicationContent/main/m/"+id+"-"+version+".zip";
            } else {
                download = "";
                success = false;
            }
        } else {
            download = url.toString();
        }
        export.set("modpack.download",download);
        export.set("modpack.instance","instances/"+id+"/");

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(export.getJsonFile());
                } catch (Exception ignore) {}
            }
        }

        creator = null;
        id = null;
        name = null;
        version = null;
        modloader = null;
        minecraft = null;
        modloaderVersion = null;
        filePath = null;
        url = null;
        json = null;
        return success;
    }

    public boolean export() {
        boolean success;
        Config json = new Config(this.json);
        String id = json.getString("modpack.id");
        if(id==null) {
            success = false;
        } else {
            success = InstanceCreator.upload(login, json.getPath(), id + ".json");
        }
        creator = null;
        this.id = null;
        name = null;
        version = null;
        modloader = null;
        minecraft = null;
        modloaderVersion = null;
        filePath = null;
        url = null;
        this.json = null;
        return success;
    }

    public void open() {
        ui = browser.getUIComponent();
        client.addFocusHandler(new CefFocusHandlerAdapter() {

            @Override
            public void onGotFocus(CefBrowser browser) {
                if (browserFocus) return;
                browserFocus = true;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                browser.setFocus(true);
            }

            @Override
            public void onTakeFocus(CefBrowser browser, boolean next) {
                browserFocus = false;
            }

        });
        getContentPane().add(ui,BorderLayout.CENTER);
        pack();
        Dimension size = new Dimension(1250,700);
        setSize(size); setMaximumSize(size); setMinimumSize(size); setResizable(false);
        setLocationRelativeTo(null);
        setIcon("/icon.png");
        getContentPane().setBackground(Color.decode("#0f0f0f"));
        setTitle("Zyneon Studios - Instance Creator");
        setVisible(true);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                CefApp.getInstance().dispose();
                dispose();
            }

        });
    }

    private void setIcon(String resourcePath) {
        try {
            setIconImage(new ImageIcon(ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(resourcePath)))).getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            System.out.println("Error obtaining icon file: " + e.getMessage());
        }
    }
}