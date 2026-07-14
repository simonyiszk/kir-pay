import lottie, { AnimationItem, RendererType } from 'lottie-web'
import { CSSProperties, useEffect, useRef } from 'react'

type LottieProps = {
  animationData: unknown
  loop?: boolean
  autoplay?: boolean
  renderer?: RendererType
  onComplete?: () => void
  className?: string
  style?: CSSProperties
}

export default function Lottie({
  animationData,
  loop = false,
  autoplay = true,
  renderer = 'svg',
  onComplete,
  className,
  style
}: LottieProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const onCompleteRef = useRef(onComplete)

  useEffect(() => {
    onCompleteRef.current = onComplete
  }, [onComplete])

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const animation: AnimationItem = lottie.loadAnimation({
      container,
      renderer,
      loop,
      autoplay,
      animationData
    })

    const unsubscribe = animation.addEventListener('complete', () => onCompleteRef.current?.())

    return () => {
      unsubscribe()
      animation.destroy()
    }
  }, [animationData, loop, autoplay, renderer])

  return <div ref={containerRef} className={className} style={style} />
}
