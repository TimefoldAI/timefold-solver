/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl.result;

import java.io.File;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.config.solver.SolverConfig;

public interface BenchmarkResult {

    public String getResultDirectoryPath();

    public File getResultDirectory();

    public boolean hasAnyFailure();

    public boolean hasAnySuccess();

    public String getName();

    public Integer getAverageUninitializedVariableCount();

    public Score getAverageScore();

}
