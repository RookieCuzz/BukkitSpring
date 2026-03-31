# BukkitSpring MyBatis Starter

Platform-agnostic MyBatis integration for BukkitSpring.

## Docs
- English: `USAGE_EN.md`
- Chinese: `USAGE_CN.md`

## Install
1. Copy `bukkitspring-starter-mybatis-*.jar` to `plugins/BukkitSpring/starters/`.
2. Restart the server.

## Config
Edit `plugins/BukkitSpring/config.yml` (section `mybatis`).

## Mapper resources
- Annotated mappers: `@Mapper` interfaces.
- XML mappers: `src/main/resources/mappers/*.xml` (extracted to `plugins/<YourPlugin>/mappers/`).
