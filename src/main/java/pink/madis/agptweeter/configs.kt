package pink.madis.agptweeter

enum class ArtifactSource(val coords: MavenCoords, val prettyName: String) {
    AGP(MavenCoords("com.android.tools.build", "gradle"), "Android Gradle Plugin");
//    SUPPORTLIB(MavenCoords("com.android.support", "support-v4"), "Android Support Library");

    fun toConfig(): Config {
        return Config(
                coords,
                TwitterConfig(
                        System.getenv("${name}_CONSUMER_KEY"),
                        System.getenv("${name}_CONSUMER_SECRET"),
                        System.getenv("${name}_ACCESS_TOKEN"),
                        System.getenv("${name}_ACCESS_TOKEN_SECRET")
                ),
                prettyName
        )
    }
}