import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { DependencyList, useEffect, useRef } from 'react'
import { filter } from 'fuzzy'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export const setPersistentState =
  <T>(key: string, stateSetter: (state: T) => void) =>
  (value: T) => {
    localStorage.setItem(key, typeof value === 'object' ? JSON.stringify(value) : String(value))
    stateSetter(value)
  }

export const useNFCScanner = (onScan: (event: NDEFReadingEvent) => void, deps: DependencyList) => {
  const onScanRef = useRef(onScan)
  useEffect(() => {
    onScanRef.current = onScan
  }, [onScan])

  useEffect(() => {
    const abortController = new AbortController()
    const ndef = new NDEFReader()
    const callback = ((e: NDEFReadingEvent) => onScanRef.current?.(e)) as EventListenerOrEventListenerObject
    ndef
      .scan({ signal: abortController.signal })
      .then(() => ndef.addEventListener('reading', callback, { signal: abortController.signal, once: true }))

    return () => abortController.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
}

const murmurHash = (x: number): number => {
  x ^= x >> 17
  x *= 0xed5ad4bb
  x ^= x >> 11
  x *= 0xac4c1b51
  x ^= x >> 15
  x *= 0x31848bab
  x ^= x >> 14
  return x
}

export const getHashedColor = (text: string): string => {
  let hash = 0
  if (text.length === 0) return '#000000'
  for (let i = 0; i < text.length; i++) {
    hash = text.charCodeAt(i) + ((hash << 5) - hash)
    hash = hash & hash
  }
  hash = murmurHash(hash)
  let color = '#'
  for (let i = 0; i < 3; i++) {
    const value = (hash >> (i * 8)) & 255
    color += value.toString(16).padStart(2, '0')
  }
  return color
}

const replaceSpecialChars = (text: string) => text.normalize('NFKD').replace(/\W/g, '')

export const fuzzySearch = <T>({ needle, haystack, getText }: { needle: string; haystack: T[]; getText: (data: T) => string }): T[] => {
  const sanitizedNeedle = replaceSpecialChars(needle.toLowerCase())
  const result = filter(sanitizedNeedle, haystack, { extract: (data) => replaceSpecialChars(getText(data).toLowerCase()) })
  return result.map((res) => res.original)
}

export const exportToCsv = async (fileName: string, csvSource: () => Promise<string>) => {
  const csv = await csvSource()
  const element = document.createElement('a')
  element.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
  element.download = fileName
  try {
    document.body.appendChild(element)
    element.click()
  } finally {
    document.body.removeChild(element)
  }
}

export const formatNumber = Intl.NumberFormat(navigator.language).format
export const formatTimestamp = Intl.DateTimeFormat(navigator.language, {
  dateStyle: 'short',
  timeStyle: 'medium'
}).format
export const compactFormat = Intl.NumberFormat(navigator.language, { notation: 'compact' }).format
