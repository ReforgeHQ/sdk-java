# Reforge SDK Logback Integration

This module provides seamless integration between the Reforge SDK and Logback, enabling dynamic log level management for your application.

## Overview

The Logback integration uses a Logback TurboFilter to intercept logging calls and dynamically determine whether they should be logged based on log level configuration from Reforge. This allows you to:

- **Centrally manage log levels** - Control logging across your entire application from the Reforge dashboard
- **Real-time updates** - Change log levels without restarting your application
- **Context-aware logging** - Different log levels for different loggers based on runtime context
- **Performance** - Efficient filtering happens before log message construction

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.reforge</groupId>
    <artifactId>sdk-logback</artifactId>
    <version>1.0.3</version>
</dependency>
```

### Compatibility

This module is compatible with:
- **Logback 1.2.x** (with SLF4J 1.7.x)
- **Logback 1.3.x** (with SLF4J 2.0.x)
- **Logback 1.4.x** (with SLF4J 2.0.x)
- **Logback 1.5.x** (with SLF4J 2.0.x)

The module uses only stable Logback APIs that haven't changed across these versions. Logback and SLF4J are marked as `provided` dependencies, so your application's versions will be used.

## Usage

### Basic Setup (Programmatic)

The simplest approach is to install the filter programmatically during application startup:

```java
import com.reforge.sdk.Sdk;
import com.reforge.sdk.Options;
import com.reforge.sdk.logback.ReforgeLogbackTurboFilter;

public class MyApplication {
    public static void main(String[] args) {
        // Initialize the Reforge SDK
        Sdk sdk = new Sdk(new Options());

        // Install the Logback turbo filter
        ReforgeLogbackTurboFilter.install(sdk.loggerClient());

        // Now all your logging will respect Reforge log levels
        Logger log = LoggerFactory.getLogger(MyApplication.class);
        log.info("Application started with dynamic log levels");
    }
}
```

**Important:** Install the filter as early as possible in your application startup, ideally right after initializing the Reforge SDK.

### Alternative: XML Configuration

You can also configure the filter in your `logback.xml`, though this requires you to make the SDK instance available statically:

```xml
<configuration>
    <turboFilter class="com.reforge.sdk.logback.ReforgeLogbackTurboFilter">
        <!-- Note: This approach requires static SDK access -->
    </turboFilter>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

**Note:** The programmatic approach is recommended because it's simpler and doesn't require static SDK access.

### How It Works

Once installed, the TurboFilter intercepts **all** logging calls across your entire application:

- Works with **all loggers** (no need to configure individual loggers)
- Works with **all appenders** (console, file, syslog, etc.)
- Filters happen **before** log messages are formatted (performance benefit)
- No modification of your existing Logback configuration needed

### Configuration

By default, the integration looks for a config key named `log-levels.default` of type `LOG_LEVEL_V2`. The configuration is evaluated with the following context:

- `reforge-sdk-logging.lang`: `"java"`
- `reforge-sdk-logging.logger-path`: The name of the logger (e.g., `"com.example.MyClass"`)

You can customize the config key:

```java
Options options = new Options().setLoggerKey("my.custom.log.config");
Sdk sdk = new Sdk(options);
ReforgeLogbackTurboFilter.install(sdk.loggerClient());
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

1. When a log statement is called, Logback's TurboFilter mechanism intercepts it before the log message is constructed
2. The filter calls `loggerClient.getLogLevel(loggerName)` to retrieve the configured log level from Reforge
3. The retrieved level is compared against the log statement's level
4. If the statement's level is high enough, it's allowed through; otherwise, it's filtered out
5. A ThreadLocal recursion guard prevents infinite loops if logging occurs during config lookup

## Performance Considerations

- **Efficient filtering**: Log level checks happen before expensive message construction
- **Recursion protection**: Built-in guard prevents performance issues from recursive logging
- **Caching**: The Reforge SDK caches configuration data to minimize network calls

## Thread Safety

The integration is fully thread-safe and uses Logback's built-in TurboFilter infrastructure, which is designed for concurrent access.

## FAQ

### Do I need to configure individual loggers?

**No!** Unlike some logging frameworks, Logback's TurboFilter mechanism works globally. Once installed, the filter automatically intercepts all logging calls across your entire application without needing to touch individual logger configurations.

### Do I need to modify my existing logback.xml?

**No!** The TurboFilter works alongside your existing Logback configuration. You don't need to change your appenders, encoders, or root logger settings.

### How does this differ from Log4j integration?

The Logback integration is simpler because:
- **Logback**: Uses TurboFilters which work at the framework level
- **Log4j**: Requires traversing and updating individual Logger objects

With Logback, there's no need to maintain or update logger hierarchies.

### Does this work with all Logback appenders?

**Yes!** The TurboFilter intercepts logging decisions before they reach any appenders. It works with:
- Console appenders
- File appenders
- Rolling file appenders
- Syslog appenders
- Async appenders
- Custom appenders

### What's the performance impact?

Minimal! The TurboFilter:
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
        ReforgeLogbackTurboFilter.install(reforgeSDK.loggerClient());
    }

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

## Troubleshooting

### Filter not installing

If you see an error message like "Unable to install ReforgeLogbackTurboFilter", ensure that:

1. Logback is actually on your classpath
2. SLF4J is bound to Logback (not another logging implementation like Log4j or java.util.logging)
3. The filter installation happens after Logback initialization

To verify Logback is being used:
```java
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;

ILoggerFactory factory = LoggerFactory.getILoggerFactory();
if (factory instanceof LoggerContext) {
    System.out.println("Using Logback!");
} else {
    System.out.println("NOT using Logback: " + factory.getClass().getName());
}
```

### Log levels not changing

If log levels aren't being respected:

1. Verify the config key exists in Reforge (default: `log-levels.default`)
2. Check that it's of type `LOG_LEVEL_V2`
3. Ensure the Reforge SDK is initialized and ready
4. Check the SDK logs for any errors during config retrieval

### Existing Logback configuration overriding dynamic levels

The TurboFilter runs before Logback's standard level checks. However, if you have very restrictive appender-level filters or threshold settings in your logback.xml, those may still apply. The TurboFilter controls whether a log event is created, but appenders can still apply their own filtering.

## Example Application

```java
package com.example;

import com.reforge.sdk.Sdk;
import com.reforge.sdk.Options;
import com.reforge.sdk.logback.ReforgeLogbackTurboFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyApplication {
    private static final Logger LOG = LoggerFactory.getLogger(MyApplication.class);

    public static void main(String[] args) {
        // Initialize Reforge SDK
        Sdk sdk = new Sdk(new Options());

        // Install Logback integration
        ReforgeLogbackTurboFilter.install(sdk.loggerClient());

        LOG.trace("This is a trace message");  // Filtered if level > TRACE
        LOG.debug("This is a debug message");  // Filtered if level > DEBUG
        LOG.info("This is an info message");   // Filtered if level > INFO
        LOG.warn("This is a warning");         // Filtered if level > WARN
        LOG.error("This is an error");         // Filtered if level > ERROR
    }
}
```
