package ru.bingosoft.teploInspector.ui.route_detail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yandex.mapkit.directions.driving.DrivingRoute
import ru.bingosoft.teploInspector.R

class DrivingRouterListAdapter(private val routes: MutableList<DrivingRoute>, private val itemListener: DrivingRouterRVClickListener): RecyclerView.Adapter<DrivingRouterListAdapter.DrivingRouterViewHolder>() {

    private lateinit var ctx: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DrivingRouterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cardview_map_bottom_sheet, parent, false)
        ctx=parent.context
        return DrivingRouterViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: DrivingRouterViewHolder,
        position: Int
    ) {
        holder.routerName.text = ctx.getString(R.string.routeName,(position+1).toString())
        holder.time.text = routes[position].metadata.weight.timeWithTraffic.text
        holder.timeWithTraffic.text = ctx.getString(R.string.time_without_traffic, routes[position].metadata.weight.time.text)
        holder.distance.text = routes[position].metadata.weight.distance.text
        holder.sectionsRoute.visibility=View.INVISIBLE

        if (routes[position].ruggedRoads.size>0) {
            holder.rugged.visibility=View.VISIBLE
        }

        holder.listener=itemListener
    }

    override fun getItemCount(): Int {
        return routes.size
    }

    class DrivingRouterViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            listener.drivingRouterRVListClicked(v, this.layoutPosition)
        }

        var routerName: TextView = itemView.findViewById(R.id.routerName)
        var time: TextView = itemView.findViewById(R.id.time)
        var distance: TextView = itemView.findViewById(R.id.transfersCount)
        var timeWithTraffic: TextView = itemView.findViewById(R.id.walkingDistance)
        var sectionsRoute: RecyclerView=itemView.findViewById(R.id.section_route_recycler_view)
        var rugged: ImageView=itemView.findViewById(R.id.ivRugged)

        lateinit var listener: DrivingRouterRVClickListener

        init {
            view.setOnClickListener(this)
        }

    }
}