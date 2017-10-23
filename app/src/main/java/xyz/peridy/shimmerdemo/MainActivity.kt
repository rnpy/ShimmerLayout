package xyz.peridy.shimmerdemo

import android.graphics.*
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

import xyz.peridy.shimmerlayout.ShimmerGroup
import xyz.peridy.shimmerlayout.ShimmerLayout
import java.util.*
import android.graphics.drawable.ColorDrawable
import com.trello.rxlifecycle2.components.RxActivity
import com.trello.rxlifecycle2.kotlin.bindToLifecycle

class MainActivity : RxActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        findViewById<RecyclerView>(R.id.recycler_view).apply {
            if (adapter == null) {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = createAdapter()
            }
        }
    }

    private fun createAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            // All shimmer views in adapter will share the same bitmaps
            private val shimmerGroup = ShimmerGroup()
            private val data: Array<Data>

            private val VIEW_HOLDER_TYPE_LOADED = 0
            private val VIEW_HOLDER_TYPE_LOADING = 1

            private inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val shimmerLayout: ShimmerLayout = itemView.findViewById(R.id.shimmer_layout)
            }

            private inner class LoadedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val textView: TextView = itemView.findViewById(R.id.text_view)
                val imageView: ImageView = itemView.findViewById(R.id.image_view)
            }

            override fun onViewRecycled(holder: RecyclerView.ViewHolder?) {
                if (holder is LoadingViewHolder) {
                    holder.shimmerLayout.visibility = View.GONE
                }
                super.onViewRecycled(holder)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val layoutId = if (viewType == VIEW_HOLDER_TYPE_LOADING) R.layout.cell_loading else R.layout.cell_loaded
                val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
                return if (viewType == VIEW_HOLDER_TYPE_LOADING) {
                    LoadingViewHolder(view).apply {
                        shimmerLayout.shimmerGroup = shimmerGroup
                    }
                } else {
                    LoadedViewHolder(view)
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (getItemViewType(position)) {
                    VIEW_HOLDER_TYPE_LOADED -> {
                        (holder as LoadedViewHolder).textView.text = "loaded element $position"
                        holder.imageView.setImageDrawable(ColorDrawable(Color.GREEN))
                    }
                    VIEW_HOLDER_TYPE_LOADING -> {
                        with((holder as LoadingViewHolder).shimmerLayout) {
                            visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun getItemCount() = data.size

            override fun getItemViewType(position: Int) =
                    if (data[position].data != null) VIEW_HOLDER_TYPE_LOADED else VIEW_HOLDER_TYPE_LOADING

            // Simulate basic sequential single thread loading (ignoring scroll)
            init {
                val adapter = this
                val rnd = Random()
                data = Array(5000, { Data(it, null) })
                Observable.fromArray(data)
                        .flatMapIterable { it.toList() }
                        .map {
                            Thread.sleep(500L + rnd.nextInt(1000))
                            return@map it
                        }
                        .subscribeOn(Schedulers.single())
                        .observeOn(AndroidSchedulers.mainThread())
                        .bindToLifecycle(this@MainActivity)
                        .forEach {
                            it.data = it.position
                            adapter.notifyItemChanged(it.position)
                        }
            }

            inner class Data(val position: Int, var data: Int?)
        }
    }
}
