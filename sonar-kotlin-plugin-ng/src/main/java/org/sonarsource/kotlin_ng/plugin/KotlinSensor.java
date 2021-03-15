/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin_ng.plugin;

import kastree.ast.Node;
import kastree.ast.psi.Parser;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.kotlin_ng.checks.CommentedCodeCheck;
import org.sonarsource.kotlin_ng.converter.KotlinCodeVerifier;

import java.io.IOException;
import java.util.regex.Pattern;

public class KotlinSensor implements Sensor {
  private static final Logger LOG = Loggers.get(KotlinSensor.class);
  private static final Pattern EMPTY_FILE_CONTENT_PATTERN = Pattern.compile("\\s*+");
  private final NoSonarFilter noSonarFilter;
  private final Language language;
  private final Checks checks;
  private FileLinesContextFactory fileLinesContextFactory;


  public KotlinSensor(CheckFactory checkFactory, NoSonarFilter noSonarFilter, FileLinesContextFactory fileLinesContextFactory, Language language) {
    this.noSonarFilter = noSonarFilter;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.language = language;

    checks = checkFactory.create(KotlinPlugin.KOTLIN_REPOSITORY_KEY);
    checks.addAnnotatedChecks((Iterable<?>) KotlinCheckList.checks());
    checks.addAnnotatedChecks(new CommentedCodeCheck(new KotlinCodeVerifier()));

  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
            .onlyOnLanguage(language.getKey())
            .name(language.getName() + " Sensor");
  }

  @Override
  public void execute(SensorContext sensorContext) {
    FileSystem fileSystem = sensorContext.fileSystem();
    FilePredicate mainFilePredicate = fileSystem.predicates().and(
            fileSystem.predicates().hasLanguage(language.getKey()),
            fileSystem.predicates().hasType(InputFile.Type.MAIN));
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(mainFilePredicate);
    analyseFiles(sensorContext, inputFiles);
  }

  private boolean analyseFiles(SensorContext sensorContext,
                               Iterable<InputFile> inputFiles) {
    for (InputFile file: inputFiles) {
      try {
        Node.File file1 = Parser.Companion.parseFile(normalizeEol(file.contents()), true);

      } catch (IOException e) {
        LOG.error(e.getMessage());
      }
    }
    return true;
  }

  @NotNull
  private static String normalizeEol(String content) {
    return content.replaceAll("\\r\\n?", "\n");
  }

}
