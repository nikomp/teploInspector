package ru.bingosoft.teploInspector.ui.map_bottom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.Section
import kotlinx.android.synthetic.main.item_cardview_map_bottom_sheet.view.*
import ru.bingosoft.teploInspector.R

class RouterListAdapter(private val routes: MutableList<Route>, private val itemListener: RouterRVClickListeners): RecyclerView.Adapter<RouterListAdapter.RouterViewHolder>() {

    private lateinit var ctx: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RouterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cardview_map_bottom_sheet, parent, false)
        ctx=parent.context
        return RouterViewHolder(view)
    }

    override fun getItemCount(): Int {
        return routes.size
    }

    override fun onBindViewHolder(holder: RouterViewHolder, position: Int) {
        holder.routerName.text = ctx.getString(R.string.routeName,(position+1).toString())
        holder.time.text = routes[position].metadata.weight.time.text
        holder.transfersCount.text =ctx.getString(R.string.transfersCount,routes[position].metadata.weight.transfersCount.toString())
        holder.walkingDistance.text = ctx.getString(R.string.walkingDistance, routes[position].metadata.weight.walkingDistance.text)

        // Уберем нулевые секции
        val sectionList= mutableListOf<Section>()
        routes[position].sections.forEach {
            if (it.metadata.weight.walkingDistance.value!=0.0 ||
                it.metadata.data.transports!=null) {
                sectionList.add(it)
            }
        }

        val flexboxLayoutManager=FlexboxLayoutManager(ctx)
        flexboxLayoutManager.flexDirection=FlexDirection.ROW
        flexboxLayoutManager.flexWrap=FlexWrap.WRAP
        flexboxLayoutManager.alignItems=AlignItems.FLEX_START
        holder.sectionsRoute.layoutManager=flexboxLayoutManager
        val adapter = SectionRouteAdapter(sectionList)
        holder.sectionsRoute.adapter = adapter


        holder.listener=itemListener
    }

    class RouterViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            listener.routerRVListClicked(v, this.layoutPosition)
        }

        var routerName: TextView = itemView.routerName
        var time: TextView = itemView.time
        var transfersCount: TextView = itemView.transfersCount
        var walkingDistance: TextView = itemView.walkingDistance
        var sectionsRoute: RecyclerView=itemView.section_route_recycler_view
        //var flexboxLayout: FlexboxLayout=itemView.flexboxlayout

        lateinit var listener: RouterRVClickListeners

        init {

            view.setOnClickListener(this)
        }

    }
}