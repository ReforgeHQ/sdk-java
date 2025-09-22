# reforge-sdk-java
Java Client for Reforge LogLevels, FeatureFlags, Config as a Service: https://reforge.com

See full documentation https://docs.reforge.com/docs/java-sdk/java

# Micronaut Support

## Context Storage

Out of the box, the Reforge client includes the ThreadLocalContextStore which relies on the same Thread handling a HTTP Request.

Micronaut has an event based model, so state must be managed without ThreadLocals - to that end we provide the `ServerRequestContextStore` that uses the ServerRequestContext.
_Note: Behind the scenes ServerRequestContext is based on a threadlocal, but micronaut's instrumentation code knows to copy this threadlocal between threads as the request moves through processing._

### Usage

Maven

```xml
<dependency>
    <groupId>com.reforge</groupId>
    <artifactId>sdk-micronaut-extension</artifactId>
    <version>0.3.26</version>
</dependency>
```

The context store implementation is added to the Reforge `Options` class.

You'll likely have a factory class like this one - see the `options.setContextStore(new ServerRequestContextStore());` in the sdk method

```java
@Factory
public class ReforgeFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ReforgeFactory.class);

    @Singleton
    public Sdk sdk(Environment environment) {
        final Options options = new Options();
        options.setContextStore(new ServerRequestContextStore());
        return new Sdk(options);
    }

    @Singleton
    public FeatureFlagClient featureFlagClient(Sdk sdk) {
        return sdk.featureFlagClient();
    }

    @Singleton
    public ConfigClient configClient(Sdk sdk) {
        return sdk.configClient();
    }
}
```

