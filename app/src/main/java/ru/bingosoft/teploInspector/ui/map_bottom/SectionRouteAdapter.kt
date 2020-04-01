package ru.bingosoft.teploInspector.ui.map_bottom

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yandex.mapkit.transport.masstransit.Section
import kotlinx.android.synthetic.main.item_cardview_route.view.*
import ru.bingosoft.teploInspector.R
import timber.log.Timber

class SectionRouteAdapter(private val sections: MutableList<Section>): RecyclerView.Adapter<SectionRouteAdapter.SectionRouteViewHolder>() {

    private lateinit var ctx: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SectionRouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cardview_route, parent, false)
        ctx=parent.context
        return SectionRouteViewHolder(view)
    }

    override fun getItemCount(): Int {
        return sections.size
    }

    override fun onBindViewHolder(
        holder: SectionRouteViewHolder,
        position: Int
    ) {
        Timber.d(sections[position].metadata.data.transports.toString())
        if (sections[position].metadata.data.transports==null) {
            Timber.d(sections[position].metadata.weight.walkingDistance.text)
            holder.sectionIcon.setImageResource(R.drawable.ic_directions_walk_black_24dp)
            holder.sectionText.text = sections[position].metadata.weight.walkingDistance.text
            holder.sectionText.movementMethod=ScrollingMovementMethod()
        } else {
            holder.sectionIcon.setImageResource(R.drawable.ic_directions_bus_black_24dp)

            var transportsName=""
            Timber.d("transportSize=${sections[position].metadata.data.transports?.size}")
            sections[position].metadata.data.transports?.forEach{
                transportsName=transportsName.plus("${it?.line?.name}, ")
            }
            transportsName=transportsName.dropLast(2)
            Timber.d("transportsName=$transportsName")


            holder.sectionText.text = transportsName
        }

        /*if (position==itemCount-1) {
            holder.nextIcon.visibility=View.INVISIBLE
        }*/



    }

    class SectionRouteViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var sectionIcon: ImageView = itemView.sectionIcon
        var sectionText: TextView = itemView.sectionText
        //var nextIcon: ImageView = itemView.nextIcon

    }
}