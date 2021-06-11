package com.gomezgimenez.timelapse.tool.threads

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

import com.github.sarxos.webcam.Webcam
import com.gomezgimenez.timelapse.tool.Util
import com.gomezgimenez.timelapse.tool.controller.{ Feature, TrackingSnapshot, WebCamSource }
import javafx.application.Platform
import javafx.concurrent.Task
import org.bytedeco.javacv.Java2DFrameUtils
import org.bytedeco.opencv.opencv_core.Point2f

import scala.collection.mutable
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Tracker(camera: WebCamSource, prefSize: Dimension, fps: Int, invalidationListener: TrackerListener) extends Task[Unit] { self =>

  case class Speed(x: Double, y: Double)

  val currentImage: AtomicReference[BufferedImage] = new AtomicReference[BufferedImage]()
  val currentFps: AtomicInteger                    = new AtomicInteger()
  val features: AtomicReference[List[Feature]]     = new AtomicReference[List[Feature]](List.empty)
  val centerOfMassSpeed: AtomicReference[Speed]    = new AtomicReference[Speed]()
  val centerOfMassMaxSpeed: AtomicReference[Speed] = new AtomicReference[Speed]()
  val highMark: AtomicReference[Option[Point2f]]   = new AtomicReference[Option[Point2f]](None)
  val lowMark: AtomicReference[Option[Point2f]]    = new AtomicReference[Option[Point2f]](None)

  def addFeature(f: Feature): Unit =
    features.getAndUpdate(_ :+ f)

  def removeFeature(id: Int): Unit =
    features.getAndUpdate(_.filterNot(_.id == id))

  override def call(): Unit = {
    val cam = Webcam.getWebcams.get(camera.index)

    try {
      if (!cam.isOpen) {
        cam.setCustomViewSizes(Array(prefSize))
        cam.setViewSize(prefSize)
        cam.open()
      }

      val maxBufferSize = 5
      val imageBuffer   = mutable.Queue.empty[TrackingSnapshot]
      var raisingEdge   = true
      var lastFrameTime = System.currentTimeMillis()
      var maxComVx      = 0.0
      var maxComVy      = 0.0

      while (!isCancelled) {
        val hrImg: BufferedImage = cam.getImage

        if (hrImg != null) {
          currentImage.set(hrImg)

          val now              = System.currentTimeMillis()
          val elapsedFrameTime = now - lastFrameTime
          val minFrameTime     = (1000.0 / fps).toLong
          val remainingTime    = Math.max(0, minFrameTime - elapsedFrameTime)
          if (remainingTime > 0) {
            Thread.sleep(remainingTime)
          }
          val lasFrameProcessingTime = now + remainingTime - lastFrameTime
          val calculatedCurrentFps   = 1000.0 / lasFrameProcessingTime

          currentFps.set(calculatedCurrentFps.toInt)

          lastFrameTime = now

          val newFeatures =
            imageBuffer.lastOption.map { lastImage =>
              Await.result(
                Future.sequence(features.get.map { feature =>
                  import com.gomezgimenez.timelapse.tool.Util._
                  import org.bytedeco.opencv.global.opencv_imgproc._
                  import org.bytedeco.opencv.opencv_core._

                  Future {
                    val sourceImgMat = Java2DFrameUtils.toMat(lastImage.img)
                    val destImgMat   = Java2DFrameUtils.toMat(hrImg)
                    val source       = new Mat()
                    val dest         = new Mat()

                    cvtColor(sourceImgMat, source, COLOR_BGRA2GRAY)
                    cvtColor(destImgMat, dest, COLOR_BGRA2GRAY)

                    val trackingStatus                = new Mat()
                    val trackedPointsNewUnfilteredMat = new Mat()
                    val err                           = new Mat()

                    import org.bytedeco.opencv.global.opencv_core._
                    import org.bytedeco.opencv.global.opencv_video._
                    import org.bytedeco.opencv.opencv_core._

                    val newTrackedPoint =
                      feature.point
                        .flatMap { p =>
                          calcOpticalFlowPyrLK(
                            source,
                            dest,
                            toMatPoint2f(Seq(p)),
                            trackedPointsNewUnfilteredMat,
                            trackingStatus,
                            err,
                            new Size(
                              feature.size.toInt,
                              feature.size.toInt
                            ),
                            5,
                            new TermCriteria(
                              CV_TERMCRIT_ITER | CV_TERMCRIT_EPS,
                              20,
                              0.03
                            ),
                            0,
                            1e-4
                          )

                          toPoint2fArray(trackedPointsNewUnfilteredMat).headOption
                            .flatMap {
                              case p if p.x > 0 && p.x < hrImg.getWidth && p.y > 0 && p.y < hrImg.getHeight =>
                                Some(p)
                              case _ => None
                            }
                        }

                    feature.copy(point = newTrackedPoint)
                  }
                }),
                30.seconds
              )
            }

          imageBuffer.enqueue(
            TrackingSnapshot(hrImg, hrImg, newFeatures.getOrElse(Set.empty).toList)
          )

          if (imageBuffer.size > maxBufferSize) {
            imageBuffer.dequeue()

            val lastCenter = imageBuffer.last
            val prevCenter = imageBuffer.dropRight(1).last
            val mcVelocity = for {
              mc1 <- Util.massCenter(lastCenter.features)
              mc2 <- Util.massCenter(prevCenter.features)
            } yield {
              (
                Math.abs((mc1.x - mc2.x) * 1000 / lasFrameProcessingTime),
                Math.abs((mc1.y - mc2.y) * 1000 / lasFrameProcessingTime)
              )
            }

            mcVelocity.foreach {
              case (vx, vy) =>
                maxComVx = Math.max(vx, maxComVx)
                maxComVy = Math.max(vy, maxComVy)
                centerOfMassSpeed.set(Speed(vx, vy))
                centerOfMassMaxSpeed.set(Speed(maxComVx, maxComVy))
            }

            val dSteps =
              (imageBuffer.toList match {
                case a :: b :: tail =>
                  tail.foldLeft(List(a -> b)) {
                    case (acc, n) => acc :+ (acc.last._2 -> n)
                  }
              }).map {
                  case (t1, t2) =>
                    (
                      Util.massCenter(t1.features),
                      Util.massCenter(t2.features)
                    )
                }
                .collect {
                  case (Some(mc1), Some(mc2)) =>
                    mc2.y - mc1.y
                }

            val dAvg = dSteps.sum / dSteps.length
            if (raisingEdge && dAvg > 0.5) {
              highMark.set(Util.massCenter(imageBuffer.head.features))
              invalidationListener.capture(hrImg)
              raisingEdge = false
            } else if (!raisingEdge && dAvg < -0.5) {
              lowMark.set(Util.massCenter(imageBuffer.head.features))
              raisingEdge = true
            }
          }

          newFeatures.foreach { nf =>
            features.getAndUpdate(
              f =>
                nf.filter(x => f.map(_.id).contains(x.id)) ++
                f.filterNot(x => nf.map(_.id).contains(x.id)))
          }

          invalidationListener.invalidate(self)
        }
      }

      cam.close()
      currentImage.lazySet(null)
      invalidationListener.invalidate(self)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        cam.close()
        currentImage.lazySet(null)
        invalidationListener.invalidate(self)
    }
  }
}
