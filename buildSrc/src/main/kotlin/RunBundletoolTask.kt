import com.android.sdklib.BuildToolInfo
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Paths

abstract class RunBundletoolTask : DefaultTask() {
    abstract class Signature() {
        object UnsignedOrDebug: Signature()
        data class Signed(val ks: String, val pass: String, val alias: String) : Signature()
    }

    @get:InputFile
    abstract val aabFile: RegularFileProperty

    /**
     * Output for build with mode=default
     */
    @get:Optional
    @get:OutputFile
    abstract val defaultApks: RegularFileProperty

    /**
     * Output for build with mode=universal
     */
    @get:Optional
    @get:OutputFile
    abstract val universalApks: RegularFileProperty

    @get:Internal
    abstract val buildToolInfo: Property<BuildToolInfo>

    @get:Internal
    abstract val signature: Property<Signature>

    @Suppress("NewApi")
    @TaskAction
    fun generateApks() {
        val aapt2Path = buildToolInfo.get().getPath(BuildToolInfo.PathId.AAPT2)
            ?: error("aapt2 not found")

        val aapt2Command = Aapt2Command.createFromExecutablePath(Paths.get(aapt2Path))
        val signature = signature.get()
        val signingConfig = if (signature is Signature.Signed) {
            SigningConfiguration.extractFromKeystore(
                java.nio.file.Path.of(signature.ks),
                signature.alias,
                java.util.Optional.of(
                    Password.createFromStringValue("pass:${signature.pass}")
                ),
                java.util.Optional.empty<Password>()
            )
        } else {
            null
        }
        if (universalApks.isPresent) {
            val outputFile = universalApks.get().asFile.toPath()

            BuildApksCommand.builder()
                .setBundlePath(aabFile.get().asFile.toPath())
                .setOutputFile(outputFile)
                .setAapt2Command(aapt2Command)
                .setApkBuildMode(BuildApksCommand.ApkBuildMode.UNIVERSAL)
                .setOverwriteOutput(true)
                .apply {
                    signingConfig?.let {
                        setSigningConfiguration(it)
                    }
                }
                .build()
                .execute()

            println("$outputFile generated")
        }
        if (defaultApks.isPresent) {
            val outputFile = defaultApks.get().asFile.toPath()

            BuildApksCommand.builder()
                .setBundlePath(aabFile.get().asFile.toPath())
                .setOutputFile(outputFile)
                .setAapt2Command(aapt2Command)
                .setApkBuildMode(BuildApksCommand.ApkBuildMode.UNIVERSAL)
                .setOverwriteOutput(true)
                .apply {
                    signingConfig?.let {
                        setSigningConfiguration(it)
                    }
                }
                .build()
                .execute()

            println("$outputFile generated")
        }
    }
}
