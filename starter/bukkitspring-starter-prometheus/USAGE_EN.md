# Prometheus Starter Usage

## Overview
This starter pushes metrics to a Prometheus Pushgateway. It uses the global
default registry (`CollectorRegistry.defaultRegistry`) so all plugins share
the same registry and only one push target is needed.

## Install
1. Put `bukkitspring-starter-prometheus-*.jar` into `plugins/BukkitSpring/starters/`
2. Restart the server

## Configuration
Edit `plugins/BukkitSpring/config.yml`:

```yaml
prometheus:
  enabled: true
  pushgateway:
    url: "http://127.0.0.1:9091"
  job: "bukkitspring"
  instance: ""
  include-jvm: true
  push-interval-ms: 15000
  push-timeout-ms: 5000
  push-mode: "push" # push or push_add
  grouping:
    server: "paper-1"
```

## Notes
- `job` is required when enabled. `instance` is optional and will be added to the grouping key.
- `push-mode=push` replaces metrics for the job+grouping key.
- `push-mode=push_add` appends metrics for the job+grouping key.
- JVM metrics are registered when `include-jvm=true`.
