/*
 * The MIT License
 *
 * Copyright (C) 2011 by Dominik Bartholdi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.joelj.jenkins.eztemplates.InheritenceStep.matrix;

import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.BuildListener;

import java.io.IOException;
import java.util.List;

/**
 * An aggregator chaining all the aggregators of the chained builders - only done/used within a matrix build.
 * 
 * @author Dominik Bartholdi (imod)
 * 
 */
public class MatrixAggregatorChain extends MatrixAggregator {

    private List<MatrixAggregator> aggregators;

    public MatrixAggregatorChain(List<MatrixAggregator> aggregators, MatrixBuild build, Launcher launcher, BuildListener listener) {
        super(build, launcher, listener);
        this.aggregators = aggregators;
    }

    @Override
    public boolean startBuild() throws InterruptedException, IOException {
        boolean shouldContinue = true;
        for (MatrixAggregator aggregator : aggregators) {
            if (!shouldContinue) {
                break;
            }
            shouldContinue = aggregator.startBuild();
        }
        return shouldContinue;
    }

    @Override
    public boolean endBuild() throws InterruptedException, IOException {
        boolean shouldContinue = true;
        for (MatrixAggregator aggregator : aggregators) {
            if (!shouldContinue) {
                break;
            }
            shouldContinue = aggregator.endBuild();
        }
        return shouldContinue;
    }

    @Override
    public boolean endRun(MatrixRun run) throws InterruptedException, IOException {
        boolean shouldContinue = true;
        for (MatrixAggregator aggregator : aggregators) {
            if (!shouldContinue) {
                break;
            }
            shouldContinue = aggregator.endRun(run);
        }
        return shouldContinue;
    }

}
