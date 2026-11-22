package ru.school28.diary

import java.awt.BasicStroke
import java.awt.Color
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/** Иконка в системном трее и нативные уведомления. */
object Tray {
    private var tray: SystemTray? = null
    private var icon: TrayIcon? = null

    fun install(
        onOpen: () -> Unit,
        onCheckNow: () -> Unit,
        onExit: () -> Unit,
    ): Boolean {
        if (!SystemTray.isSupported()) {
            AppLog.log("tray: SystemTray не поддерживается")
            return false
        }
        return try {
            val systemTray = SystemTray.getSystemTray()
            val trayIcon = TrayIcon(createImage(), "ЛинТех 28 — Дневник")
            trayIcon.isImageAutoSize = true

            val menu = PopupMenu()
            MenuItem("Открыть").apply { addActionListener { onOpen() } }.also(menu::add)
            MenuItem("Проверить дневник").apply { addActionListener { onCheckNow() } }.also(menu::add)
            menu.addSeparator()
            MenuItem("Выход").apply { addActionListener { onExit() } }.also(menu::add)
            trayIcon.popupMenu = menu
            trayIcon.addActionListener { onOpen() }

            systemTray.add(trayIcon)
            tray = systemTray
            icon = trayIcon
            AppLog.log("tray: установлен")
            true
        } catch (e: Exception) {
            AppLog.logException("tray/install", e)
            false
        }
    }

    fun notify(title: String, text: String) {
        val trayIcon = icon ?: return
        try {
            trayIcon.displayMessage(title, text, TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            AppLog.logException("tray/notify", e)
        }
    }

    fun notifyMinimized() {
        notify("ЛинТех 28", "Приложение свёрнуто в трей и продолжит проверять дневник.")
    }

    fun remove() {
        try {
            icon?.let { tray?.remove(it) }
        } catch (e: Exception) {
            AppLog.logException("tray/remove", e)
        }
        icon = null
        tray = null
    }

    /** Простая иконка: лиловый квадрат с белыми строками дневника. */
    private fun createImage(size: Int = 32): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color(0x95, 0x75, 0xCD)
            g.fillRoundRect(2, 1, size - 4, size - 2, 9, 9)
            g.color = Color.WHITE
            g.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            val left = 9
            val right = size - 9
            for (i in 0 until 4) {
                val y = 10 + i * 4
                g.drawLine(left, y, if (i == 3) left + 6 else right, y)
            }
        } finally {
            g.dispose()
        }
        return image
    }
}
