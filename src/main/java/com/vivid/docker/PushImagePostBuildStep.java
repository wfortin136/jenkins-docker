package com.vivid.docker;

import com.vivid.docker.argument.*;
import com.vivid.docker.command.*;
import com.vivid.docker.exception.*;
import com.vivid.docker.helper.*;
import hudson.*;
import hudson.model.*;
import org.kohsuke.stapler.*;

public class PushImagePostBuildStep extends DockerPostBuildStep {

    private final String image;
    private final String tag;
    private final String buildTrigger;
    private final boolean disableContentTrust;
    private final boolean fail;

    @DataBoundConstructor
    public PushImagePostBuildStep(String image,
                                  String tag,
                                  String buildTrigger,
                                  String alternativeDockerHost,
                                  boolean disableContentTrust,
                                  boolean fail) {
        super(alternativeDockerHost);
        this.image = image;
        this.tag = tag;
        this.disableContentTrust = disableContentTrust;
        this.fail = fail;
        this.buildTrigger = buildTrigger;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        if (shouldTriggerBuild(build)) {
            try {

                boolean result = false;

                EnvVars environment = getEnvironment(build, listener);

                String tagName = FieldHelper.getMacroReplacedFieldValue(tag, environment);

                for (String t : tagName.split(",")) {

                    PushCommandArgumentBuilder pushCommandArgumentBuilder = new PushCommandArgumentBuilder()
                            .disableContentTrust(disableContentTrust)
                            .image(String.format("%s:%s", image, t.trim().toLowerCase()));

                    listener.getLogger().append(String.format("Pushing \"%s:%s\" to Docker Hub.", image, t.trim().toLowerCase()));

                    DockerCommandExecutor command = new DockerCommandExecutor(pushCommandArgumentBuilder, environment);
                    result = command.execute(build, launcher, listener);

                    if (!result) {
                        break;
                    }
                }

                return result;

            } catch (EnvironmentConfigurationException e) {
                launcher.getListener().fatalError(String.format("Error: %s\n", e.getMessage()));
                return false;
            }
        }
        return true;
    }

    private boolean shouldTriggerBuild(AbstractBuild build) {
        switch (buildTrigger) {
            case "SUCCESS":
                if (build.getResult() == Result.SUCCESS || build.getResult() == Result.UNSTABLE) {
                    return true;
                }
            case "FAILURE":
                return build.getResult() == Result.FAILURE;
        }
        return false;
    }

    public String getImage() {
        return image;
    }

    public String getTag() {
        return tag;
    }

    public boolean isFail() {
        return fail;
    }

    public boolean isDisableContentTrust() {
        return disableContentTrust;
    }

    public String getBuildTrigger() {
        return buildTrigger;
    }
}

