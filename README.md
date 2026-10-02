# keyshin-java

Java 17+ client for [KeyShin](https://github.com/kishin-dev/KeyShin) license validation. No dependencies.

## Installation

[![](https://jitpack.io/v/kishin-dev/keyshin-java.svg)](https://jitpack.io/#kishin-dev/keyshin-java)

Requires Java 17+.

### Maven

Add the JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<dependency>
    <groupId>com.github.kishin-dev</groupId>
    <artifactId>keyshin-java</artifactId>
    <version>v1.0.0</version>
</dependency>
```

### Gradle (Groovy)

In `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

In `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.kishin-dev:keyshin-java:v1.0.0'
}
```

### Gradle (Kotlin DSL)

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

In `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.kishin-dev:keyshin-java:v1.0.0")
}
```

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
