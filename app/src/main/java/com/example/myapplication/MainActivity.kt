package ru.sumin.servicestest

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.ComponentName
import android.content.ServiceConnection
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var page = 0

    private val serviceConnection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? MyForegroundService.LocalBinder) ?: return
            val foregroundService = service.getService()
            foregroundService.onProgressChanged = { progress ->
                binding.progressBar.progress = progress
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.alarmManager.setOnClickListener {
            val alarmManger = getSystemService(ALARM_SERVICE) as AlarmManager
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, 5)
            val intent = AlarmReceiver.newIntent(this)
            val pendingIntent = PendingIntent.getBroadcast(this, 100, intent, 0)
            alarmManger.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }


        binding.simpleService.setOnClickListener {
            stopService(MyForegroundService.newIntent(this))
            startService(MyService.newIntent(this, 25))
        }
        binding.foregroundService.setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                MyForegroundService.newIntent(this)
            )
        }
        binding.intentService.setOnClickListener {
            ContextCompat.startForegroundService(
                this,
                MyIntentService.newIntent(this)
            )
        }
        binding.jobDispatcher.setOnClickListener {
            val componentName = ComponentName(this, MyJobService::class.java)

            val jobInfo = JobInfo.Builder(MyJobService.JOB_ID, componentName)
                //.setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build()

            val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = MyJobService.newIntent(page++)
                jobScheduler.enqueue(jobInfo, JobWorkItem(intent))
            }else{
                startService(MyIntentService2.newIntent(this,page++))
            }
        }
        binding.jobIntentService.setOnClickListener {
            MyJobIntentService.enqueue(this,page++)
        }
        binding.workManager.setOnClickListener {
            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueueUniqueWork(
                MyWorker.NAME,
                ExistingWorkPolicy.APPEND,
                MyWorker.makeRequest(page++)

            )
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(MyForegroundService.newIntent(this), serviceConnection, 0 )
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }
}
