type Props = {
  title: string
  message: string
}

export function StatePanel({ title, message }: Props) {
  return (
    <div className="state-panel" role="status">
      <h2>{title}</h2>
      <p>{message}</p>
    </div>
  )
}
