/*
 * Copyright (c) 2017  STMicroelectronics – All rights reserved
 * The STMicroelectronics corporate logo is a trademark of STMicroelectronics
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 *    and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * - Neither the name nor trademarks of STMicroelectronics International N.V. nor any other
 *    STMicroelectronics company nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * - All of the icons, pictures, logos and other images that are provided with the source code
 *    in a directory whose title begins with st_images may only be used for internal purposes and
 *    shall not be redistributed to any third party or modified in any way.
 *
 * - Any redistributions in binary form shall not include the capability to display any of the
 *    icons, pictures, logos and other images that are provided with the source code in a directory
 *    whose title begins with st_images.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package com.st.smartTag.tagPlotData

import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.st.smartTag.NfcTagViewModel
import com.st.smartTag.R
import com.st.smartTag.SmartTagService
import com.st.smartTag.model.DataSample

import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.widget.ProgressBar


class TagDataFragment : Fragment() {

    private val nfcServiceResponse = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                SmartTagService.READ_TAG_NUMBER_SAMPLE_DATA_ACTION -> {
                    val numberSample = intent.getIntExtra(SmartTagService.EXTRA_TAG_NUMBER_SAMPLE, 0)
                    //Toast.makeText(context, String.format("sample: %d", numberSample), Toast.LENGTH_SHORT).show()
                    smartTag.setNumberSample(numberSample)
                }
                SmartTagService.READ_TAG_SAMPLE_DATA_ACTION -> {
                    val data = intent.getParcelableExtra<DataSample>(SmartTagService.EXTRA_TAG_SAMPLE_DATA)
                    smartTag.appendSample(data)
                }
                SmartTagService.READ_TAG_ERROR_ACTION -> {
                    val msg = intent.getStringExtra(SmartTagService.EXTRA_ERROR_STR)
                    readProgress.visibility=View.GONE // Hide the progres after an error
                    nfcTagHolder.nfcTagError(msg)
                }
            }
        }
    }

    private lateinit var nfcTagHolder: NfcTagViewModel
    private lateinit var smartTag: TagDataViewModel
    private lateinit var readProgress: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_show_data, container, false)
        rootView.findViewById<ViewPager>(R.id.tagData_viewPager).adapter = TabViewAdapter(childFragmentManager)
        readProgress = rootView.findViewById(R.id.tagData_readProgress)
     /*   mAdapter = new MyAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);*/
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        smartTag = TagDataViewModel.create(activity!!)
        nfcTagHolder = NfcTagViewModel.create(activity!!)
        initializeNfcTagObserver()
    }

    private fun <T> getProgressUpdate():Observer<T>{
        return Observer<T> {
            val nReadSample = smartTag.allSampleList.value?.size ?: 0
            val nTotalSample = smartTag.numberSample.value ?: 0
            Log.d("progress","$nReadSample / $nTotalSample")
            if(nReadSample>=nTotalSample){
                readProgress.visibility = View.GONE
            }else{
                readProgress.progress = nReadSample
            }
        }
    }

    private fun initializeNfcTagObserver() {
        nfcTagHolder.nfcTag.observe(this, Observer {
            if (it != null) SmartTagService.startReadingDataSample(context!!, it)
        })
        smartTag.numberSample.observe(this, Observer {
            it?.let {
                Log.d("progress","Total: "+it)
                readProgress.max = it
                readProgress.visibility = View.VISIBLE
            }
        })
        smartTag.lastSensorSample.observe(this,getProgressUpdate())
        smartTag.lastEventSample.observe(this,getProgressUpdate())

    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(context!!)
                .registerReceiver(nfcServiceResponse, SmartTagService.getReadDataSampleFilter())
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context!!)
                .unregisterReceiver(nfcServiceResponse)
    }


    private class TabViewAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        companion object {
            val FRAGMENT_VIEW = arrayOf(
                    TagPlotDataFragment::class.java,
                    TagEventDataFragment::class.java
            )
        }

        override fun getCount(): Int {
            return FRAGMENT_VIEW.size
        }

        override fun getItem(position: Int): Fragment {
            return FRAGMENT_VIEW[position].newInstance()
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return if (position==0)
                "Sensor Plots"
            else
                "Events"
        }
    }
}