package com.monstercontroller.bukkitspring.dependency;

import java.util.Objects;

public final class MavenDependency {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public MavenDependency(String groupId, String artifactId, String version) {
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
        this.version = Objects.requireNonNull(version, "version");
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public String fileName() {
        return artifactId + "-" + version + ".jar";
    }

    public String relativePath() {
        return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + fileName();
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
