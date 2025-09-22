# Reforge Java SDK

[![javadoc](https://javadoc.io/badge2/com.reforge/sdk/javadoc.svg)](https://javadoc.io/doc/com.reforge/sdk)
[![Better Uptime Badge](https://betteruptime.com/status-badges/v1/monitor/pdi9.svg)](https://betteruptime.com/?utm_source=status_badge)

Java (11+) Reforge SDK for FeatureFlags, Config as a Service: https://reforge.com

See full documentation [https://docs.reforge.com/docs/java-sdk/java](https://docs.reforge.com/docs/sdks/java)

Maven
```xml
<dependency>
    <groupId>com.reforge</groupId>
    <artifactId>sdk</artifactId>
    <version>0.3.26</version>
</dependency>
```


## Contributing to reforge-sdk-java

* Check out the latest `main` to make sure the feature hasn't been implemented or the bug hasn't been fixed yet.
* Check out the issue tracker to make sure someone already hasn't requested it and/or contributed it.
* Fork the project.
* Start a feature/bugfix branch.
* Fetch the submodules with `git submodule init` and `git submodule update`
* Run tests with `mvn test` to ensure everything is in a good state.
* Commit and push until you are happy with your contribution.
* Make sure to add tests for it. This is important so we don't break it in a future version unintentionally.

If you get errors about the pom not being sorted, run `mvn sortpom:sort -Dsort.createBackupFile=false`
If you get errors about the code not being formatted, run `mvn prettier:write`

## Copyright

Copyright (c) 2025 Reforge. See LICENSE for further details.
