/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.zimManager.libraryView.adapter

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.widget.Toast
import androidx.annotation.StringRes
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.extensions.setBitmap
import org.kiwix.kiwixmobile.core.extensions.setTextAndVisibility
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.zim_manager.KiloByte
import org.kiwix.kiwixmobile.databinding.ItemDownloadBinding
import org.kiwix.kiwixmobile.databinding.ItemLibraryBinding
import org.kiwix.kiwixmobile.databinding.LibraryDividerBinding
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.kiwixmobile.zimManager.libraryView.AvailableSpaceCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.LibraryDownloadItem

sealed class LibraryViewHolder<in T : LibraryListItem>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class LibraryBookViewHolder(
    private val itemLibraryBinding: ItemLibraryBinding,
    private val bookUtils: BookUtils,
    private val clickAction: (BookItem) -> Unit,
    private val availableSpaceCalculator: AvailableSpaceCalculator
  ) : LibraryViewHolder<BookItem>(itemLibraryBinding.root) {
    override fun bind(item: BookItem) {
      itemLibraryBinding.libraryBookTitle.setTextAndVisibility(item.book.title)
      itemLibraryBinding.libraryBookDescription.setTextAndVisibility(item.book.description)
      itemLibraryBinding.libraryBookCreator.setTextAndVisibility(item.book.creator)
      itemLibraryBinding.libraryBookDate.setTextAndVisibility(item.book.date)
      itemLibraryBinding.libraryBookSize.setTextAndVisibility(
        KiloByte(item.book.size).humanReadable
      )
      itemLibraryBinding.libraryBookLanguage.text = bookUtils.getLanguage(item.book.language)
      itemLibraryBinding.libraryBookFavicon.setBitmap(Base64String(item.book.favicon))

      val hasAvailableSpaceInStorage = availableSpaceCalculator.hasAvailableSpaceForBook(item.book)
      containerView.setOnClickListener { clickAction.invoke(item) }
      containerView.isClickable =
        item.canBeDownloaded && hasAvailableSpaceInStorage

      itemLibraryBinding.tags.render(item.tags)

      itemLibraryBinding.unableToDownload.visibility =
        if (item.canBeDownloaded && hasAvailableSpaceInStorage)
          View.GONE
        else
          View.VISIBLE
      itemLibraryBinding.unableToDownload.setOnLongClickListener {
        when (item.fileSystemState) {
          CannotWrite4GbFile -> it.centreToast(R.string.file_system_does_not_support_4gb)
          DetectingFileSystem -> it.centreToast(R.string.detecting_file_system)
          else -> {
            if (item.canBeDownloaded && !hasAvailableSpaceInStorage) {
              clickAction.invoke(item)
            } else {
              throw RuntimeException("impossible invalid state: ${item.fileSystemState}")
            }
          }
        }
        true
      }
    }
  }

  class DownloadViewHolder(
    private val itemDownloadBinding: ItemDownloadBinding,
    private val clickAction: (LibraryDownloadItem) -> Unit
  ) :
    LibraryViewHolder<LibraryDownloadItem>(itemDownloadBinding.root) {

    override fun bind(item: LibraryDownloadItem) {
      itemDownloadBinding.libraryDownloadFavicon.setBitmap(item.favIcon)
      itemDownloadBinding.libraryDownloadTitle.text = item.title
      itemDownloadBinding.libraryDownloadDescription.text = item.description
      itemDownloadBinding.downloadProgress.progress = item.progress
      itemDownloadBinding.stop.setOnClickListener { clickAction.invoke(item) }
      itemDownloadBinding.downloadState.text =
        item.downloadState.toReadableState(containerView.context)
      if (item.currentDownloadState == Status.FAILED) {
        clickAction.invoke(item)
      }
      itemDownloadBinding.eta.text = item.readableEta
    }
  }

  class LibraryDividerViewHolder(private val libraryDividerBinding: LibraryDividerBinding) :
    LibraryViewHolder<DividerItem>(libraryDividerBinding.root) {
    override fun bind(item: DividerItem) {
      libraryDividerBinding.dividerText.setText(item.stringId)
    }
  }
}

@SuppressLint("ShowToast")
private fun View.centreToast(@StringRes id: Int) {
  val locationXAndY = intArrayOf(0, 0)
  getLocationOnScreen(locationXAndY)
  val midX = locationXAndY[0] + width / 2
  val midY = locationXAndY[1] + height / 2
  Toast.makeText(context, id, Toast.LENGTH_LONG).apply {
    view?.let { view ->
      view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
      setGravity(
        Gravity.TOP or Gravity.START,
        midX - view.measuredWidth / 2,
        midY - view.measuredHeight
      )
    }
  }.show()
}
