package in.dragonbra.javasteamsamples._007_loginwithproxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.networking.steam3.ProtocolTypes;
import in.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails;
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback;
import in.dragonbra.javasteam.steam.steamclient.SteamClient;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;
import in.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration;
import in.dragonbra.javasteam.util.NetHookNetworkListener;
import in.dragonbra.javasteam.util.log.DefaultLogListener;
import in.dragonbra.javasteam.util.log.LogManager;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author jaymie
 * @since 2025-03-14
 */
@SuppressWarnings("FieldCanBeLocal")
public class SampleLoginWithProxy implements Runnable {

    private SteamClient steamClient;

    private CallbackManager manager;

    private SteamUser steamUser;

    private boolean isRunning;

    private final String user;

    private final String webLogonNonce;

    private List<Closeable> subscriptions;

    private Proxy proxy;


    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public SampleLoginWithProxy(String user, String webLogonNonce) {
        this.user = user;
        this.webLogonNonce = webLogonNonce;
    }

    public static void main(String[] args) throws InterruptedException {
        // you can view https://steamcommunity.com/chat/clientjstoken to get webLogonNonce, the property name is "token"
        // webLoginNonce can only use one time, it's more safety for your customer

        if (args.length < 2) {
            System.out.println("Sample1: No username and password specified!");
            return;
        }

        LogManager.addListener(new DefaultLogListener());

        SampleLoginWithProxy client = new SampleLoginWithProxy(args[0], args[1]);
        // if you use http proxy, you can only use websocket protocol to connection, you need to modify the configuration

        Proxy proxy = new Proxy(Proxy.Type.SOCKS,
                new InetSocketAddress("127.0.0.1", 7890));

//        Proxy proxy = new Proxy(Proxy.Type.HTTP,
//                new InetSocketAddress("127.0.0.1", 7890));

        client.setProxy(proxy);
        client.run();

        Thread.sleep(1000000L);
    }

    @Override
    public void run() {

        // If any configuration needs to be set; such as connection protocol api key, etc., you can configure it like so.
        var configuration = SteamConfiguration.create(config -> {
            config.withProtocolTypes(ProtocolTypes.TCP)
                    .withProxy(proxy);
        });
        // create our steamclient instance with custom configuration.
        steamClient = new SteamClient(configuration);

        // create our steamclient instance using default configuration
//        steamClient = new SteamClient();

        steamClient.setDebugNetworkListener(new NetHookNetworkListener());

        // create the callback manager which will route callbacks to function calls
        manager = new CallbackManager(steamClient);

        // get the steamuser handler, which is used for logging on after successfully connecting
        steamUser = steamClient.getHandler(SteamUser.class);

        // The callbacks are a closeable, and to properly fix
        // "'Closeable' used without 'try'-with-resources statement", they should be closed once done.
        // Usually putting them in a list and close each of them once the client is finished is recommended.
        subscriptions = new ArrayList<>();

        // register a few callbacks we're interested in
        // these are registered upon creation to a callback manager, which will then route the callbacks
        // to the functions specified
        subscriptions.add(manager.subscribe(ConnectedCallback.class, this::onConnected));
        subscriptions.add(manager.subscribe(DisconnectedCallback.class, this::onDisconnected));
        subscriptions.add(manager.subscribe(LoggedOnCallback.class, this::onLoggedOn));
        subscriptions.add(manager.subscribe(LoggedOffCallback.class, this::onLoggedOff));

        isRunning = true;

        System.out.println("Connecting to steam...");

        // initiate the connection
        steamClient.connect();

        // create our callback handling loop
        while (isRunning) {
            // in order for the callbacks to get routed, they need to be handled by the manager
            manager.runWaitCallbacks(1000L);
        }

        // Close the subscriptions when done.
        System.out.println("Closing " + subscriptions.size() + " callbacks");
        for (var subscription : subscriptions) {
            try {
                subscription.close();
            } catch (IOException e) {
                System.out.println("Couldn't close a callback.");
            }
        }
    }

    @SuppressWarnings("DanglingJavadoc")
    private void onConnected(ConnectedCallback callback) {
        System.out.println("Connected to Steam! Logging in " + user + "...");

        LogOnDetails details = new LogOnDetails();
        details.setUsername(user);
        details.setWebLogonNonce(webLogonNonce);

        // Set LoginID to a non-zero value if you have another client connected using the same account,
        // the same private ip, and same public ip.
        details.setLoginID(149);

        steamUser.logOn(details);
    }

    private void onDisconnected(DisconnectedCallback callback) {
        System.out.println("Disconnected from Steam. User initialized: " + callback.isUserInitiated());

        // If the disconnection was not user initiated, we will retry connecting to steam again after a short delay.
        if (callback.isUserInitiated()) {
            isRunning = false;
        } else {
            try {
                Thread.sleep(2000L);
                steamClient.connect();
            } catch (InterruptedException e) {
                System.err.println("An Interrupted exception occurred. " + e.getMessage());
            }
        }
    }

    private void onLoggedOn(LoggedOnCallback callback) {
        if (callback.getResult() != EResult.OK) {
            System.out.println("Unable to logon to Steam: " + callback.getResult() + " / " + callback.getExtendedResult());

            isRunning = false;
            return;
        }

        System.out.println("Successfully logged on!");

        // at this point, we'd be able to perform actions on Steam

        // for this sample we'll just log off
        steamUser.logOff();
    }

    private void onLoggedOff(LoggedOffCallback callback) {
        System.out.println("Logged off of Steam: " + callback.getResult());

        isRunning = false;
    }


    @SuppressWarnings("unused")
    private void parseJsonWebToken(String token, String name) {
        String[] tokenComponents = token.split("\\.");

        // Fix up base64url to normal base64
        String base64 = tokenComponents[1].replace('-', '+').replace('_', '/');

        if (base64.length() % 4 != 0) {
            base64 += new String(new char[4 - base64.length() % 4]).replace('\0', '=');
        }

        byte[] payloadBytes = Base64.getDecoder().decode(base64);

        // Payload can be parsed as JSON, and then fields such expiration date, scope, etc can be accessed
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement payload = JsonParser.parseString(new String(payloadBytes));
        String formatted = gson.toJson(payload);

        // For brevity, we will simply output formatted json to console
        System.out.println(name + ": " + formatted);
        System.out.println();
    }
}
