/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.UnresolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.internal.artifacts.transform.ArtifactTransforms;
import org.gradle.api.internal.artifacts.transform.VariantSelector;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.specs.Spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BuildDependenciesOnlyVisitedArtifactSet implements VisitedArtifactSet {
    private final Configuration configuration;
    private final Set<UnresolvedDependency> unresolvedDependencies;
    private final VisitedArtifactsResults artifactsResults;
    private final VisitedFileDependencyResults fileDependencyResults;
    private final ArtifactTransforms artifactTransforms;

    public BuildDependenciesOnlyVisitedArtifactSet(Configuration configuration, Set<UnresolvedDependency> unresolvedDependencies, VisitedArtifactsResults artifactsResults, VisitedFileDependencyResults fileDependencyResults, ArtifactTransforms artifactTransforms) {
        this.configuration = configuration;
        this.unresolvedDependencies = unresolvedDependencies;
        this.artifactsResults = artifactsResults;
        this.fileDependencyResults = fileDependencyResults;
        this.artifactTransforms = artifactTransforms;
    }

    @Override
    public SelectedArtifactSet select(Spec<? super Dependency> dependencySpec, AttributeContainerInternal requestedAttributes, Spec<? super ComponentIdentifier> componentSpec, boolean allowNoMatchingVariant) {
        VariantSelector variantSelector = artifactTransforms.variantSelector(requestedAttributes, allowNoMatchingVariant);
        ResolvedArtifactSet selectedArtifacts = artifactsResults.select(componentSpec, variantSelector).getArtifacts();
        ResolvedArtifactSet selectedFiles = fileDependencyResults.select(componentSpec, variantSelector).getArtifacts();
        return new BuildDependenciesOnlySelectedArtifactSet(configuration, unresolvedDependencies, selectedArtifacts, selectedFiles);
    }

    private static class BuildDependenciesOnlySelectedArtifactSet implements SelectedArtifactSet {
        private final Configuration configuration;
        private final Set<UnresolvedDependency> unresolvedDependencies;
        private final ResolvedArtifactSet selectedArtifacts;
        private final ResolvedArtifactSet selectedFiles;

        BuildDependenciesOnlySelectedArtifactSet(Configuration configuration, Set<UnresolvedDependency> unresolvedDependencies, ResolvedArtifactSet selectedArtifacts, ResolvedArtifactSet selectedFiles) {
            this.configuration = configuration;
            this.unresolvedDependencies = unresolvedDependencies;
            this.selectedArtifacts = selectedArtifacts;
            this.selectedFiles = selectedFiles;
        }

        @Override
        public void collectBuildDependencies(BuildDependenciesVisitor visitor) {
            if (!unresolvedDependencies.isEmpty()) {
                List<Throwable> failures = new ArrayList<Throwable>();
                for (UnresolvedDependency unresolvedDependency : unresolvedDependencies) {
                    failures.add(unresolvedDependency.getProblem());
                }
                visitor.visitFailure(new ResolveException(configuration.toString(), failures));
            }
            selectedArtifacts.collectBuildDependencies(visitor);
            selectedFiles.collectBuildDependencies(visitor);
        }

        @Override
        public void visitArtifacts(ArtifactVisitor visitor) {
            throw new UnsupportedOperationException("Artifacts have not been resolved.");
        }

    }
}
