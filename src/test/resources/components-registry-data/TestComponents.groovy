import static org.octopusden.octopus.escrow.BuildSystem.*

"components-registry-service-client" {
    componentDisplayName = "Components Registry Service Client"
    componentOwner = "jdoe"
    releaseManager = "jdoe"
    securityChampion = "jdoe"
    groupId = "org.octopusden.octopus.infrastructure"
    system = "NONE"
    jira {
        projectKey = "CREG"
        majorVersionFormat = '$major.$minor'
        releaseVersionFormat = '$major.$minor.$service'
    }
    vcsSettings {
        vcsUrl = "git@github.com:octopusden/octopus-components-registry-service.git"
    }
    distribution {
        explicit = true
        external = true
    }
}

"octopus-security-common" {
    componentDisplayName = "Octopus Security Common"
    componentOwner = "jdoe"
    releaseManager = "jdoe"
    securityChampion = "jdoe"
    groupId = "org.octopusden.octopus-cloud-commons"
    system = "NONE"
    jira {
        projectKey = "SEC"
        majorVersionFormat = '$major.$minor'
        releaseVersionFormat = '$major.$minor.$service'
    }
    vcsSettings {
        vcsUrl = "git@github.com:octopusden/octopus-cloud-commons.git"
    }
    distribution {
        explicit = true
        external = true
    }
}

"versions-api" {
    componentDisplayName = "Octopus Versions Api"
    componentOwner = "jdoe"
    releaseManager = "jdoe"
    securityChampion = "jdoe"
    groupId = "org.octopusden.octopus.releng"
    system = "NONE"
    jira {
        projectKey = "VER"
        majorVersionFormat = '$major.$minor'
        releaseVersionFormat = '$major.$minor.$service'
    }
    vcsSettings {
        vcsUrl = "git@github.com:octopusden/octopus-versions-api.git"
    }
    distribution {
        explicit = true
        external = true
    }
}