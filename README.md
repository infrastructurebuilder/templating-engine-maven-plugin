# templating-engine-maven-plugin

A plugin that processes files using a templating engine from `templating-engine-root`.

Usurped in part from [the original project](https://github.com/nativelibs4java/maven-velocity-plugin) 
Modified to conform to our release standards

## Usage

```
  <build>
    <plugins>
      <plugin>
        <groupId>org.infrastructurebuilder.templating</groupId>
        <artifactId>templating-engine-maven-plugin</artifactId>
        <version>whaever</version>
        <configuration>
          <executionIdentifier>exec</executionIdentifier>
          <properties>
            <foo>bar</foo>
          </properties>
        </configuration>
      </plugin>
      ...
    </plugins>
  </build>
```

