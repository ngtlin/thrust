package nl.ngti.thrust.model

import com.sun.xml.internal.fastinfoset.util.StringArray

data class JokeCto(val id: Int, val joke: String, val categories: StringArray)

data class IcndbJoke(val type: String, val joke: JokeCto)
