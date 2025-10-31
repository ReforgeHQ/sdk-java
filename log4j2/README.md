# Reforge SDK Log4j2 Integration

This module provides seamless integration between the Reforge SDK and Apache Log4j2, enabling dynamic log level management for your application.

## Overview

The Log4j2 integration uses an AbstractFilter to intercept logging calls and dynamically determine whether they should be logged based on log level configuration from Reforge. This allows you to:

- **Centrally manage log levels** - Control logging across your entire application from the Reforge dashboard
- **Real-time updates** - Change log levels without restarting your application
- **Context-aware logging** - Different log levels for different loggers based on runtime context
- **Performance** - Efficient filtering happens before log message construction

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.reforge</groupId>
    <artifactId>sdk-log4j2</artifactId>
    <version>1.0.3</version>
</dependency>
```

### Compatibility

This module is compatible with:
- **Log4j2 2.x** - The module uses only stable Log4j2 Core APIs (AbstractFilter, LoggerContext) that have been stable across Log4j2 2.x versions
- **SLF4J 1.7.x and 2.0.x** (both work with Log4j2)

**Note:** Log4j2 and SLF4J are marked as `provided` dependencies - your application's versions will be used automatically. We compile against Log4j2 2.19.0, but the APIs used are stable across the 2.x line. Log4j2 works with both SLF4J 1.7.x and 2.0.x.

## Usage

### Basic Setup (Programmatic)

The simplest approach is to install the filter programmatically during application startup:

```java
import com.reforge.sdk.Sdk;
import com.reforge.sdk.Options;
import com.reforge.sdk.log4j2.ReforgeLog4j2Filter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyApplication {
    private static final Logger log = LogManager.getLogger(MyApplication.class);

    public static void main(String[] args) {
        // Initialize the Reforge SDK
        Sdk sdk = new Sdk(new Options());

        // Install the Log4j2 filter
        ReforgeLog4j2Filter.install(sdk.loggerClient());

        // Now all your logging will respect Reforge log levels
        log.info("Application started with dynamic log levels");
    }
}
```

**Important:**
- Install the filter as early as possible in your application startup, ideally right after initializing the Reforge SDK
- Install after Log4j2 is initialized
- Any dynamic reconfiguration of Log4j2 (e.g., via JMX or programmatic changes) will remove the filter and it will need to be reinstalled

### Configuration

By default, the integration looks for a config key named `log-levels.default` of type `LOG_LEVEL_V2`. The configuration is evaluated with the following context:

- `reforge-sdk-logging.lang`: `"java"`
- `reforge-sdk-logging.logger-path`: The name of the logger (e.g., `"com.example.MyClass"`)

You can customize the config key:

```java
Options options = new Options().setLoggerKey("my.custom.log.config");
Sdk sdk = new Sdk(options);
ReforgeLog4j2Filter.install(sdk.loggerClient());
```

### Example Reforge Configuration

In your Reforge dashboard, create a `LOG_LEVEL_V2` config with key `log-levels.default`:

```yaml
# Default to INFO for all loggers
default: INFO

# Set specific packages to DEBUG
rules:
  - criteria:
      logger-path:
        starts-with: "com.example.services"
    value: DEBUG

  # Only log errors in noisy third-party library
  - criteria:
      logger-path:
        starts-with: "com.thirdparty.noisy"
    value: ERROR
```

## How It Works

Once installed, the filter intercepts **all** logging calls across your entire application:

- Works with **all loggers** (no need to configure individual loggers)
- Works with **all appenders** (console, file, rolling, syslog, async, etc.)
- Filters happen **before** log messages are formatted (performance benefit)
- No modification of your existing Log4j2 configuration needed

## Performance Considerations

- **Efficient filtering**: Log level checks happen before expensive message construction
- **Recursion protection**: Built-in guard prevents performance issues from recursive logging
- **Caching**: The Reforge SDK caches configuration data to minimize network calls

## Thread Safety

The integration is fully thread-safe and uses Log4j2's built-in filter infrastructure, which is designed for concurrent access.

## FAQ

### Do I need to configure individual loggers?

**No!** The filter works at the LoggerContext level and automatically intercepts all logging calls across your entire application without needing to configure individual loggers.

### Do I need to modify my existing log4j2.xml?

**No!** The filter works alongside your existing Log4j2 configuration. You don't need to change your appenders, layouts, or logger settings.

### Does this work with all Log4j2 appenders?

**Yes!** The filter intercepts logging decisions before they reach any appenders. It works with:
- Console appenders
- File appenders
- Rolling file appenders
- Syslog appenders
- Async appenders
- JDBC appenders
- Custom appenders

### What's the performance impact?

Minimal! The filter:
- Runs before log message formatting (avoiding expensive string operations for filtered logs)
- Uses a ThreadLocal recursion guard to prevent infinite loops
- Only makes one SDK call per log statement
- The SDK caches configuration data to minimize network overhead

### Can I use this in Spring Boot applications?

**Yes!** Just install the filter in your main application class or a `@PostConstruct` method:

```java
@SpringBootApplication
public class MyApplication {
    @Autowired
    private Sdk reforgeSDK;

    @PostConstruct
    public void setupLogging() {
        ReforgeLog4j2Filter.install(reforgeSDK.loggerClient());
    }

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### What happens if Log4j2 is reconfigured?

If Log4j2 is dynamically reconfigured (via JMX, programmatic reconfiguration, or automatic file watching), the filter will be removed. You'll need to reinstall it after reconfiguration. Consider adding a reconfiguration listener if your application uses dynamic reconfiguration.

## Troubleshooting

### Filter not installing

If you see errors during filter installation, ensure that:

1. Log4j2 is actually on your classpath
2. You're using Log4j2 (not Log4j 1.x or another logging implementation)
3. The filter installation happens after Log4j2 initialization

To verify Log4j2 is being used:
```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

try {
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    System.out.println("Using Log4j2: " + context.getClass().getName());
} catch (ClassCastException e) {
    System.out.println("NOT using Log4j2 Core");
}
```

### Log levels not changing

If log levels aren't being respected:

1. Verify the config key exists in Reforge (default: `log-levels.default`)
2. Check that it's of type `LOG_LEVEL_V2`
3. Ensure the Reforge SDK is initialized and ready
4. Check the SDK logs for any errors during config retrieval
5. Verify the filter is still installed (it may have been removed by reconfiguration)

### Existing Log4j2 configuration overriding dynamic levels

The filter runs as part of Log4j2's filter chain. However, if you have:
- Appender-level filters with thresholds
- Very restrictive context-wide filters
- Logger-level settings that are more restrictive

These may still apply. The filter controls whether a log event is created at all, but downstream filters and appender settings can still block events.

### Filter removed after reconfiguration

If your application uses Log4j2's configuration file watching or programmatic reconfiguration, the filter will be removed. Options:

1. **Disable automatic reconfiguration** if not needed
2. **Add a reconfiguration listener** that reinstalls the filter
3. **Install via configuration** (though this requires static SDK access)

## Example Application

```java
package com.example;

import com.reforge.sdk.Sdk;
import com.reforge.sdk.Options;
import com.reforge.sdk.log4j2.ReforgeLog4j2Filter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyApplication {
    private static final Logger log = LogManager.getLogger(MyApplication.class);

    public static void main(String[] args) {
        // Initialize Reforge SDK
        Sdk sdk = new Sdk(new Options());

        // Install Log4j2 integration
        ReforgeLog4j2Filter.install(sdk.loggerClient());

        log.trace("This is a trace message");  // Filtered if level > TRACE
        log.debug("This is a debug message");  // Filtered if level > DEBUG
        log.info("This is an info message");   // Filtered if level > INFO
        log.warn("This is a warning");         // Filtered if level > WARN
        log.error("This is an error");         // Filtered if level > ERROR
        log.fatal("This is fatal");            // Filtered if level > FATAL
    }
}
```

## Differences from Logback Integration

The Log4j2 integration differs from Logback in a few ways:

| Feature | Log4j2 | Logback |
|---------|--------|---------|
| Filter type | AbstractFilter | TurboFilter |
| Filter scope | LoggerContext-wide | Framework-wide |
| Reconfiguration | Filter removed on reconfig | Filter persists |
| FATAL level | Native support | Maps to ERROR |

Both integrations provide the same dynamic log level functionality with similar performance characteristics.
