# Release

1. Release commit: set version=x.y.z in gradle.properties and change action.yaml to:
    ```yaml
    image: 'docker://ghcr.io/spring-io/central-publish-action:vx.y.z'
    ```
2. Push tag vx.y.z. The workflow pushes the image and creates the GitHub release.
3. Next development commit: leave `action.yaml` as it is, so main keeps pointing at the released image.
