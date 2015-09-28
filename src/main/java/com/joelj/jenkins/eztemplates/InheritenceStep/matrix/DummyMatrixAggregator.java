package com.joelj.jenkins.eztemplates.InheritenceStep.matrix;

import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.BuildListener;

import java.io.IOException;

public class DummyMatrixAggregator extends MatrixAggregator {

    public DummyMatrixAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        super(build, launcher, listener);
    }

    @Override
    public boolean endBuild() throws InterruptedException, IOException {
        return super.endBuild();
    }

    @Override
    public boolean startBuild() throws InterruptedException, IOException {
        return super.startBuild();
    }

    @Override
    public boolean endRun(MatrixRun run) throws InterruptedException, IOException {
        return super.endRun(run);
    }
}