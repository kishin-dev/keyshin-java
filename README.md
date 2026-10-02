# keyshin-java

Java 17+ client for [KeyShin](https://github.com/kishin-dev/KeyShin) license validation. No dependencies.

## Install (Maven, via JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.kishin-dev</groupId>
    <artifactId>keyshin-java</artifactId>
    <version>v1.0.0</version>
</dependency>
```

Gradle: `implementation 'com.github.kishin-dev:keyshin-java:v1.0.0'` with `maven { url 'https://jitpack.io' }`.

## Usage

```java
import lol.kishin.keyshin.KeyShinClient;

boolean ok = KeyShinClient.validate("https://licenses.kishin.lol", licenseKey, "discord-bot-pro");
```

`validate` returns `true` only if the server says the key is valid. Anything else, including network errors, returns `false`.

To register the machine as an activation, pass a fingerprint and label:

```java
KeyShinClient.validate(url, licenseKey, "discord-bot-pro", machineFingerprint, hostname);
```

To tell "invalid" apart from "couldn't check" and show the customer the reason:

```java
try {
    ValidationResult r = KeyShinClient.check(url, licenseKey, "discord-bot-pro");
    if (!r.valid()) {
        System.out.println(r.message()); // e.g. "License has expired."
    }
} catch (KeyShinException e) {
    // server unreachable / 5xx: maybe allow a grace period instead of shutting down
}
```

Calls are blocking (8s timeout), so run them off your main/game thread.
