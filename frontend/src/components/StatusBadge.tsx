type Props = {
  assessment: string
}

export function StatusBadge({ assessment }: Props) {
  const tone =
    assessment === 'Delivered'
      ? 'ok'
      : assessment === 'Missed' || assessment === 'Expired'
        ? 'bad'
        : assessment === 'Partial'
          ? 'mid'
          : 'open'

  return <span className={`badge badge-${tone}`}>{assessment}</span>
}
