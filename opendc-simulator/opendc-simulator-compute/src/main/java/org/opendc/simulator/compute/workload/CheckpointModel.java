/*
 * Copyright (c) 2024 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.simulator.compute.workload;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// CheckPoint Model
// TODO: Move this to a separate file
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import java.time.InstantSource;
import org.jetbrains.annotations.NotNull;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;

public class CheckpointModel extends FlowNode {
    private SimWorkload simWorkload;
    private long checkpointInterval;
    private final long checkpointDuration;
    private double checkpointIntervalScaling;
    private FlowGraph graph;

    private long startOfInterval;

    public CheckpointModel(@NotNull SimWorkload simWorkload) {
        super(simWorkload.getGraph());

        this.checkpointInterval = simWorkload.getCheckpointInterval();
        this.checkpointDuration = simWorkload.getCheckpointDuration();
        this.checkpointIntervalScaling = simWorkload.getCheckpointIntervalScaling();
        this.simWorkload = simWorkload;

        this.graph = simWorkload.getGraph();

        InstantSource clock = graph.getEngine().getClock();

        this.startOfInterval = clock.millis();
    }

    @Override
    public long onUpdate(long now) {
        if (this.simWorkload == null) {
            return Long.MAX_VALUE;
        }

        long passedTime = now - startOfInterval;
        long remainingTime = this.checkpointInterval - passedTime;

        // Interval not completed
        if (remainingTime > 0) {
            return now + remainingTime;
        }

        simWorkload.makeSnapshot(now);

        // start new fragment
        this.startOfInterval = now - passedTime;

        // Scale the interval time between checkpoints based on the provided scaling
        this.checkpointInterval = (long) (this.checkpointInterval * this.checkpointIntervalScaling);

        return now + this.checkpointInterval + this.checkpointDuration;
    }

    public void start() {
        this.invalidate();
    }

    public void close() {
        this.closeNode();

        this.simWorkload = null;
        this.graph = null;
    }
}
