# TeamApps Configuration Management

### Key features
- Define config options easily via code including **default values** and **comments**
- Generated config file classes
- Auto generated config xml files including **default values** and **comments**
- Automatically apply program arguments and environment variables to the config
- Web UI to change config while running
- Event registration to be notified about config changes
- Auto update the local config xml file if modified via UI
- If run in cluster, automatically switch to cluster mode and allow cluster wide config

### Services and apps
- Each service or app has its own config file
- The name of the file is **serviceName.xml**

### Possible ways for service or app configuration
- Environment variables
- Program arguments
- Local config file (xml file)
- TeamApps configuration UI
- Per code (define defaults and the possible config options)

### Configuration hierarchy
From lowest to highest
- Default value
- Config file
- Environment variable
- Program argument
- **Global** value from active cluster (if used)

### Naming and Case-sensitivity:
- All names are **case-insensitive**
- Config file names are case-insensitive
- Hierarchies can be expressed
  - Environment variables with **double underscores**: SERVICE_NAME__KEY_NAME=VALUE
  - Program arguments with **dots**: --serviceName.keyName=value

