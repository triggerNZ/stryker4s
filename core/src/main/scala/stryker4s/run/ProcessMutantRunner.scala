package stryker4s.run

import java.nio.file.Path

import better.files.File
import grizzled.slf4j.Logging
import stryker4s.config.Config
import stryker4s.model._
import stryker4s.run.process.ProcessRunner

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ProcessMutantRunner(process: ProcessRunner)(implicit config: Config)
    extends MutantRunner
    with Logging {

  override def apply(files: Iterable[MutatedFile]): MutantRunResults = {
    val startTime = System.currentTimeMillis()
    val tmpDir = File.newTemporaryDirectory("stryker4s-")
    debug("Using temp directory: " + tmpDir)

    config.baseDir.copyTo(tmpDir)

    // Overwrite files to mutated files
    files foreach {
      case MutatedFile(file, tree, _) =>
        val subPath = config.baseDir.relativize(file)
        val filePath = tmpDir / subPath.toString
        filePath.overwrite(tree.syntax)
    }

    val totalMutants = files.flatMap(_.mutants).flatMap(_.mutants).size

    val runResults = for {
      mutatedFile <- files
      subPath = config.baseDir.relativize(mutatedFile.fileOrigin)
      registeredMutant <- mutatedFile.mutants
      mutant <- registeredMutant.mutants
    } yield {
      val result = runMutant(mutant, tmpDir, subPath)
      val id = mutant.id
      info(
        s"Finished mutation run $id/$totalMutants (${((id / totalMutants.toDouble) * 100).round}%)")
      result
    }

    val duration = Duration(System.currentTimeMillis() - startTime, MILLISECONDS)
    MutantRunResults(runResults, duration)
  }

  private def runMutant(mutant: Mutant, workingDir: File, subPath: Path): MutantRunResult = {
    val id = mutant.id
    info(s"Starting test-run $id...")
    process("sbt test", workingDir, ("ACTIVE_MUTATION", id.toString)) match {
      case Success(exitCode) if exitCode == 0 => Survived(mutant, subPath)
      case Success(exitCode)                  => Killed(exitCode, mutant, subPath)
      case Failure(exc: TimeoutException)     => TimedOut(exc, mutant, subPath)
    }
  }
}