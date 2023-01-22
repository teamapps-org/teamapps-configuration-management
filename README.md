# TeamApps Configuration Management

## Key features
- Define config options easily via code including **default values** and **comments**
- Generated config file classes (Java classes) via **teamapps-message-protocol**
- Auto generated config xml files including **default values** and **comments**
- Automatically apply program arguments and environment variables to the config
- Web UI to change config while running
- REST-API to retrieve or update config
- Event registration to be notified about config changes
- Auto update the local config xml file if modified via UI or REST-API
- If run in cluster, automatically switch to cluster mode and allow cluster wide config
- Option to track config changes in a persisted update store

### Services and apps
- Each service or app has its own config file
- The name of the file is **serviceName.xml**

### Possible ways for service or app configuration
- Environment variables
- Program arguments
- Local config file (xml file)
- TeamApps configuration UI
- Per code (define defaults and the possible config options)
- REST-API: retrieve and update config

### Configuration hierarchy
From lowest to highest
- Default value
- Config file
- Environment variable
- Program argument
- REST-API config update
- **Global** value from active cluster (if used)

### Cluster mode
In cluster mode update rules can be overridden:
- Certain config keys or a full service config can be switched to cluster mode
- A config key in cluster mode  
  - will automatically be distributed to all members in the cluster
  - can be protected from local updates (e.g. config file updates or REST-API updates)

### Naming and Case-sensitivity:
- All names are **case-insensitive**
- Config file names are case-insensitive
- Hierarchies can be expressed
  - Environment variables with **double underscores**: SERVICE_NAME__KEY_NAME=VALUE
    - E.g.: TESTAPP__AUTH_CONFIG__SECRET=secret
  - Program arguments with **dots**: --serviceName.keyName=value
    - E.g.: testApp.authConfig.secret=secret

### Default values
- Local config files will be searched in "./config/*.xml"
- Config path can be updated via:
  - Environment variable: TEAMAPPS__CONFIG_PATH=path
  - Program argument: --teamApps.configPath=path

## How to use
