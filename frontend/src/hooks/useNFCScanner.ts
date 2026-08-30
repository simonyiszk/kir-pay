import { DependencyList, useEffect, useRef, useState } from 'react'

export const useNFCScanner = (onScan: (event: NDEFReadingEvent) => void, deps: DependencyList) => {
  const onScanRef = useRef(onScan)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    onScanRef.current = onScan
  }, [onScan])

  useEffect(() => {
    const abortController = new AbortController()
    const ndef = new NDEFReader()
    const callback = ((e: NDEFReadingEvent) => onScanRef.current?.(e)) as EventListenerOrEventListenerObject
    ndef
      .scan({ signal: abortController.signal })
      .then(() => {
        setError(null)
        ndef.addEventListener('reading', callback, { signal: abortController.signal })
      })
      .catch((err: Error) => {
        if (err.name === 'AbortError') return
        setError(err.name === 'NotAllowedError' ? 'NFC nem elérhető / engedély megtagadva' : err.message || 'NFC hiba')
      })

    return () => abortController.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { error }
}
