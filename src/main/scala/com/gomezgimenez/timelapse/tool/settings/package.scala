package com.gomezgimenez.timelapse.tool

package object settings {
  import com.gomezgimenez.timelapse.tool.controller.Resolution

  case class Settings(
      version: String,
      trackingSettings: Option[TrackingSettings] = None,
      playerSettings: Option[PlayerSettings] = None
  )

  sealed trait SettingsResolution
  case class SettingsCustomResolution(width: Int, height: Int) extends SettingsResolution
  case class SettingsFixedResolution(width: Int, height: Int)  extends SettingsResolution

  case class TrackingSettings(
      camera: String,
      resolution: SettingsResolution
  )

  case class PlayerSettings()
}
