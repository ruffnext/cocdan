import { IStage } from "../../../../../bindings/entity/basic/IStage"

type Props = {
  stage: IStage
}

export default (props: Props) => {
  return (
    <div class="flex flex-col-reverse w-full h-full overflow-y-auto">
      <div class="flex w-full h-12 text-lg mt-2 mb-2">
        <input type="text" class="flex-grow ml-4 rounded-l-md border-solid border-l-2 border-t-2 border-b-2 border-r-0 pl-2" />
        <button class="border-solid border-t-2 border-b-2">
          <svg class="h-8 w-8" width="24" height="24" viewBox="0 0 24 24" stroke-width="2"
            stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
            <path stroke="none" d="M0 0h24v24H0z" />
            <line x1="10" y1="14" x2="21" y2="3" />
            <path d="M21 3L14.5 21a.55 .55 0 0 1 -1 0L10 14L3 10.5a.55 .55 0 0 1 0 -1L21 3" />
          </svg>
        </button>
        <button class="border-solid border-r-2 border-t-2 border-b-2 border-l-0 rounded-r-md mr-2">
          <svg class="h-8 w-8 " fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
          </svg>
        </button>
      </div>
    </div>
  )
}