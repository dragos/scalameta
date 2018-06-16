package scala.meta.internal.scalacp

import scala.meta.internal.{semanticdb3 => s}
import scala.meta.internal.semanticdb3.Accessibility.{Tag => a}
import scala.meta.internal.semanticdb3.{Language => l}
import scala.meta.internal.semanticdb3.Scala._
import scala.meta.internal.semanticdb3.Scala.{Descriptor => d}
import scala.meta.internal.semanticdb3.SymbolInformation.{Kind => k}

object Synthetics {
  def setterInfos(getterInfo: s.SymbolInformation, linkMode: LinkMode): List[s.SymbolInformation] = {
    val getterSym = getterInfo.symbol
    val setterSym = {
      if (getterSym.isGlobal) {
        val setterName = getterInfo.name + "_="
        Symbols.Global(getterSym.owner, d.Method(setterName, "()"))
      } else {
        getterSym + "+1"
      }
    }

    val paramSym = {
      if (getterSym.isGlobal) Symbols.Global(setterSym, d.Parameter("x$1"))
      else getterSym + "+2"
    }
    val paramTpe = getterInfo.tpe match {
      case s.MethodType(_, _, sret) => sret
      case _ => s.NoType
    }
    val paramInfo = s.SymbolInformation(
      symbol = paramSym,
      language = l.SCALA,
      kind = k.PARAMETER,
      properties = 0,
      name = "x$1",
      tpe = paramTpe,
      annotations = Nil,
      accessibility = Some(s.Accessibility(a.PUBLIC)))

    val setterTpe = {
      val unit = s.TypeRef(s.NoType, "scala.Unit#", Nil)
      val setterParamss = {
        linkMode match {
          case SymlinkChildren => List(s.Scope(symlinks = List(paramInfo.symbol)))
          case HardlinkChildren => List(s.Scope(hardlinks = List(paramInfo)))
        }
      }
      s.MethodType(Some(s.Scope()), setterParamss, unit)
    }
    val setterInfo = s.SymbolInformation(
      symbol = setterSym,
      language = l.SCALA,
      kind = k.METHOD,
      properties = getterInfo.properties,
      name = getterInfo.name + "_=",
      tpe = setterTpe,
      annotations = getterInfo.annotations,
      accessibility = getterInfo.accessibility)

    linkMode match {
      case SymlinkChildren => List(paramInfo, setterInfo)
      case HardlinkChildren => List(setterInfo)
    }
  }
}