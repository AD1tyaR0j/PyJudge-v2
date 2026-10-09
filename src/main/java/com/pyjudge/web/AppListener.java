package com.pyjudge.web;

import com.pyjudge.AppContext;
import com.pyjudge.config.AppConfig;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application lifecycle. On deploy it connects to the database, creates the tables, loads the
 * first-run data and publishes the {@link AppContext} for the servlets; on undeploy it stops the
 * judge workers and closes the connection pool.
 */
@WebListener
public class AppListener implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(AppListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            AppConfig config = AppConfig.load();
            loadDriver(config.get("db.url"));
            AppContext app = AppContext.start(config);
            event.getServletContext().setAttribute(AppContext.ATTRIBUTE, app);
            LOG.info(() -> "PyJudge started with database " + app.db().getUrl().replaceAll("\\?.*", ""));
        } catch (RuntimeException e) {
            // Leave the attribute unset: every servlet then reports that the app did not start
            LOG.log(Level.SEVERE, "PyJudge could not start. Check db.url, db.user and db.password in "
                    + "application.properties, and that the database server is running.", e);
        }
    }

    /** Inside a servlet container JDBC drivers in WEB-INF/lib are not always auto-registered, so load ours by name. */
    private static void loadDriver(String url) {
        String driver = url.startsWith("jdbc:h2:") ? "org.h2.Driver"
                : url.startsWith("jdbc:mariadb:") ? "org.mariadb.jdbc.Driver"
                : url.startsWith("jdbc:mysql:") ? "com.mysql.cj.jdbc.Driver" : null;
        if (driver != null) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                LOG.warning("JDBC driver " + driver + " is not on the classpath; relying on DriverManager.");
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object app = event.getServletContext().getAttribute(AppContext.ATTRIBUTE);
        if (app instanceof AppContext context) {
            context.close();
        }
        // Unregister the drivers this web application loaded, so redeploying does not leak its class loader
        ClassLoader mine = Thread.currentThread().getContextClassLoader();
        for (Driver driver : Collections.list(DriverManager.getDrivers())) {
            if (driver.getClass().getClassLoader() == mine) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    LOG.log(Level.FINE, "Could not deregister " + driver, e);
                }
            }
        }
    }
}
