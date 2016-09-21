# gradle-rabbit-plugin
a RabbitMQ gradle plugin for integration tests with RabbitMQ

[ ![Build Status](https://travis-ci.org/amirkibbar/cherry.svg?branch=master) ](https://travis-ci.org/amirkibbar/cherry)
[ ![Download](https://api.bintray.com/packages/amirk/maven/gradle-rabbit-plugin/images/download.svg) ](https://bintray.com/amirk/maven/gradle-rabbit-plugin/_latestVersion)

# Using

Plugin setup for gradle >= 2.1:

```gradle

    plugins {
        id "ajk.gradle.rabbit" version "0.0.4"
    }
```

Plugin setup for gradle < 2.1:

```gradle

    buildscript {
        repositories {
            jcenter()
            maven { url "http://dl.bintray.com/amirk/maven" }
        }
        dependencies {
            classpath("ajk.gradle.rabbit:gradle-rabbit-plugin:0.0.4")
        }
    }

    apply plugin: 'ajk.gradle.rabbit'
```

# Starting and stopping RabbitMQ during the integration tests

```gradle

    task integrationTests(type: Test) {
        reports {
            html.destination "$buildDir/reports/integration-tests"
        }

        include "**/*IT.*"

        doFirst {
            startRabbit {
                rabbitVersion = "3.5.2"
                erlangVersion = "17.5
                nodePort = 5672
                mgmtPort = 15672
                epmdPort = 4639
            }
        }
    
        doLast {
            stopRabbit {}
        }
    }
    
    gradle.taskGraph.afterTask { Task task, TaskState taskState ->
        if (task.name == "integrationTests") {
            stopRabbit {}
        }
    }

    test {
        include '**/*Test.*'
        exclude '**/*IT.*'
    }
```

The above example shows a task called integrationTests which runs all the tests in the project with the IT suffix. The
reports for these tests are placed in the buildDir/reports/integration-tests directory - just to separate them from
regular tests. But the important part here is in the doFirst and doLast. 

In the doFirst RabbitMQ is started. All the values in the example above are the default values, so if these values
work for you they can be omitted:

```gradle

    doFirst {
        startRabbit {}
    }
```

In the doLast RabbitMQ is stopped. Note that RabbitMQ is also stopped in the gradle.taskGraph.afterTask section - this
is to catch any crashes during the integration tests and make sure that RabbitMQ is stopped in the build clean phase.

Lastly the regular test task is configured to exclude the tests with the IT suffix - we only wanted to run these in the
integration tests phase, not with the regular tests.

# More configuration

When running on windows this plugin installs Erlang if it can't find it in the projectDir/gradle/tools/erlang directory.
On linux installing Erlang requires root access, so you have to install it manually, here's how to install Erlang on
Ubuntu, for example:

```bash

    wget http://packages.erlang-solutions.com/ubuntu/erlang_solutions.asc
    sudo apt-key add erlang_solutions.asc
    echo "deb http://packages.erlang-solutions.com/ubuntu trusty contrib" sudo tee /etc/apt/sources.list.d/erlang.list
    echo "deb http://packages.erlang-solutions.com/ubuntu saucy contrib" sudo tee -a /etc/apt/sources.list.d/erlang.list
    echo "deb http://packages.erlang-solutions.com/ubuntu precise contrib" sudo tee -a /etc/apt/sources.list.d/erlang.list
    sudo apt-get update
    sudo apt-get install -y --force-yes erlang
```

If you don't want the plugin to install Erlang for you (either on windows or linux), then you can set the erlangHome
property to the location of your Erlang installation.

The plugin will always install RabbitMQ locally in projectDir/gradle/tools/rabbit on both windows and linux.

# References

- [Erlang](http://www.erlang.org/)
- [RabbitMQ](http://www.rabbitmq.com/)